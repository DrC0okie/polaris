#ifndef DISPLAY_TEXT_COMMAND_H
#define DISPLAY_TEXT_COMMAND_H

#include <ArduinoJson.h>

#include <string>

#include "icommand.h"
#include "utils/display_controller.h"

class DisplayTextCommand : public ICommand {
public:
    DisplayTextCommand(DisplayController& displayController, const JsonObject& params);
    void execute() override;

private:
    DisplayController& _displayController;
    std::string _text;
    uint8_t _size;
    bool _centered;
};

#endif  // DISPLAY_TEXT_COMMAND_H