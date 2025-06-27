#include "stop_blink_command.h"

#include <HardwareSerial.h>

StopBlinkCommand::StopBlinkCommand(LedController& ledController) : _ledController(ledController) {
}

void StopBlinkCommand::execute() {
    Serial.println("[Command] Executing STOP_BLINK.");
    _ledController.stopBlinking();
}