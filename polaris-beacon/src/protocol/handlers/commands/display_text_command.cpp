#include "display_text_command.h"

#include <HardwareSerial.h>

DisplayTextCommand::DisplayTextCommand(DisplayController& displayController,
                                       const JsonObject& params)
    : _displayController(displayController) {
    if (params.isNull()) {
        Serial.println("[Command] DisplayText created with no parameters.");
        _text = "No params!";
        _size = 1;
        _centered = false;
        return;
    }

    // Use default values if keys are missing
    _text = params["text"] | "Default Text";
    _size = params["size"] | 1;
    _centered = params["centered"] | false;

    Serial.printf("[Command] DisplayText parsed params: Text='%s', Size=%u, Centered=%d\n",
                  _text.c_str(), _size, _centered);
}

void DisplayTextCommand::execute() {
    Serial.println("[Command] Executing DISPLAY_TEXT.");
    _displayController.showMessage(_text, _size, _centered);
}