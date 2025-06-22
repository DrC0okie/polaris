#include "reboot_command.h"
#include <Arduino.h>

void RebootCommand::execute() {
    Serial.println("[Command] Executing REBOOT. Restarting in 3 seconds...");
    delay(3000);
    ESP.restart();
}