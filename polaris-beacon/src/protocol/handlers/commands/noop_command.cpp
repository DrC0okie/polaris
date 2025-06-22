#include "noop_command.h"

#include <HardwareSerial.h>

void NoOpCommand::execute() {
    Serial.println("[Command] Executing NO_OP.");
}