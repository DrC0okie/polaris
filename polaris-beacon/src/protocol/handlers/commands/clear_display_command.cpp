#include "clear_display_command.h"

#include <HardwareSerial.h>

ClearDisplayCommand::ClearDisplayCommand(DisplayController& displayController)
    : _displayController(displayController) {
}

CommandResult ClearDisplayCommand::execute() {
    Serial.println("[Command] Executing CLEAR_DISPLAY.");
    _displayController.clear();

    CommandResult result;
    result.success = true;
    return result;
}