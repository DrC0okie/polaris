#include "stop_blink_command.h"

#include <HardwareSerial.h>

StopBlinkCommand::StopBlinkCommand(LedController& ledController) : _ledController(ledController) {
}

CommandResult StopBlinkCommand::execute() {
    Serial.println("[Command] Executing STOP_BLINK.");
    _ledController.stopBlinking();

    CommandResult result;
    result.success = true;
    return result;
}