#include "noop_command.h"

#include <HardwareSerial.h>

CommandResult NoOpCommand::execute() {
    Serial.println("[Command] Executing NO_OP.");

    CommandResult result;
    result.success = true;
    return result;
}