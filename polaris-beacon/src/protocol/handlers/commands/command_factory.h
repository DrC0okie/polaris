#ifndef COMMAND_FACTORY
#define COMMAND_FACTORY

#include <ArduinoJson.h>

#include <memory>

#include "icommand.h"
#include "protocol/handlers/outgoing_message_service.h"
#include "utils/display_controller.h"
#include "utils/led_controller.h"
#include "utils/system_monitor.h"

class CommandFactory {
public:
    CommandFactory(LedController& ledController, DisplayController& displayController,
                   SystemMonitor& systemMonitor, OutgoingMessageService& outgoingMessageService);

    std::unique_ptr<ICommand> createCommand(OperationType opType, const JsonObject& params);

private:
    LedController& _ledController;
    DisplayController& _displayController;
    SystemMonitor& _systemMonitor;
    OutgoingMessageService& _outgoingMessageService;
};
#endif  // COMMAND_FACTORY