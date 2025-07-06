#include "reboot_command.h"
#include <Arduino.h>

CommandResult RebootCommand::execute() {
    Serial.println("[Command] Executing REBOOT. Restarting in 3 seconds...");
    delay(3000);
    ESP.restart();

    CommandResult result;
    result.success = true;
    return result;
}