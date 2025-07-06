#ifndef COMMAND_FACTORY
#define COMMAND_FACTORY

#include <ArduinoJson.h>

#include <memory>

#include "../encrypted_message_handler.h"
#include "icommand.h"
#include "protocol/handlers/outgoing_message_service.h"
#include "utils/display_controller.h"
#include "utils/led_controller.h"
#include "utils/system_monitor.h"

// Forward declarations
class KeyManager;
class EncryptedMessageHandler;

/**
 * @class CommandFactory
 * @brief Creates concrete command objects based on an operation type.
 */
class CommandFactory {
public:
    /**
     * @brief Constructs the CommandFactory.
     * @param ledController Reference to the LED controller.
     * @param displayController Reference to the OLED display controller.
     * @param systemMonitor Reference to the system status monitoring utility.
     * @param outgoingMessageService Reference to the service for queuing outgoing messages.
     */
    CommandFactory(LedController& ledController, DisplayController& displayController,
                   SystemMonitor& systemMonitor, OutgoingMessageService& outgoingMessageService);

    /**
     * @brief Creates a command object.
     * @param opType The operation type that determines which command to create.
     * @param params A JsonObject containing parameters for the command. Can be null.
     * @return A `unique_ptr` to the created command object, or `nullptr` if the opType is unknown.
     */
    std::unique_ptr<ICommand> createCommand(OperationType opType, const JsonObject& params,
                                            KeyManager& keyManager);

private:
    /// @brief Reference to the LED hardware controller.
    LedController& _ledController;

    /// @brief Reference to the display hardware controller.
    DisplayController& _displayController;

    /// @brief Reference to the system monitoring utility.
    SystemMonitor& _systemMonitor;

    /// @brief Reference to the outgoing message queuing service.
    OutgoingMessageService& _outgoingMessageService;
};
#endif  // COMMAND_FACTORY