#include "noop_command.h"

#include <HardwareSerial.h>

NoOpCommand::NoOpCommand(SystemEventNotifier& notifier) : _notifier(notifier){}

CommandResult NoOpCommand::execute() {
    Serial.println("[Command] Executing NO_OP.");
    _notifier.notify(SystemEventType::ServerCmd_NoOp);

    CommandResult result;
    result.success = true;
    return result;
}