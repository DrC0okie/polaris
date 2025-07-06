#include "command_factory.h"

#include "blink_led_command.h"
#include "clear_display_command.h"
#include "display_text_command.h"
#include "noop_command.h"
#include "reboot_command.h"
#include "request_status_command.h"
#include "rotate_key_finish_command.h"
#include "rotate_key_init_command.h"
#include "stop_blink_command.h"

CommandFactory::CommandFactory(LedController& ledController, DisplayController& displayController,
                               SystemMonitor& systemMonitor,
                               OutgoingMessageService& outgoingMessageService)
    : _ledController(ledController),
      _displayController(displayController),
      _systemMonitor(systemMonitor),
      _outgoingMessageService(outgoingMessageService) {
}

std::unique_ptr<ICommand> CommandFactory::createCommand(OperationType opType,
                                                        const JsonObject& params,
                                                        KeyManager& keyManager) {
    switch (opType) {
        case OperationType::NoOp:
            return std::unique_ptr<NoOpCommand>(new NoOpCommand());

        case OperationType::Reboot:
            return std::unique_ptr<RebootCommand>(new RebootCommand());

        case OperationType::BlinkLed:
            return std::unique_ptr<BlinkLedCommand>(new BlinkLedCommand(_ledController, params));

        case OperationType::StopBlink:
            return std::unique_ptr<StopBlinkCommand>(new StopBlinkCommand(_ledController));

        case OperationType::DisplayText:
            return std::unique_ptr<DisplayTextCommand>(
                new DisplayTextCommand(_displayController, params));

        case OperationType::ClearDisplay:
            return std::unique_ptr<ClearDisplayCommand>(
                new ClearDisplayCommand(_displayController));

        case OperationType::RequestBeaconStatus:
            return std::unique_ptr<RequestStatusCommand>(
                new RequestStatusCommand(_systemMonitor, _outgoingMessageService));

        case OperationType::RotateKeyInit:
            return std::unique_ptr<RotateKeyInitCommand>(new RotateKeyInitCommand(keyManager));

        case OperationType::RotateKeyFinish:
            return std::unique_ptr<RotateKeyFinishCommand>(new RotateKeyFinishCommand(keyManager));

        default:
            // For unknown commands, return a null pointer.
            // The caller is responsible for handling this.
            return nullptr;
    }
}