#include "command_factory.h"

#include "blink_led_command.h"
#include "noop_command.h"
#include "reboot_command.h"
#include "stop_blink_command.h"

CommandFactory::CommandFactory(LedController& ledController) : _ledController(ledController) {
}

std::unique_ptr<ICommand> CommandFactory::createCommand(OperationType opType,
                                                        const JsonObject& params) {
    switch (opType) {
        case OperationType::NoOp:
            return std::unique_ptr<NoOpCommand>(new NoOpCommand());

        case OperationType::Reboot:
            return std::unique_ptr<RebootCommand>(new RebootCommand());

        case OperationType::BlinkLed:
            return std::unique_ptr<BlinkLedCommand>(new BlinkLedCommand(_ledController, params));

        case OperationType::StopBlink:
            return std::unique_ptr<StopBlinkCommand>(new StopBlinkCommand(_ledController));

        default:
            // For unknown commands, return a null pointer.
            // The caller is responsible for handling this.
            return nullptr;
    }
}