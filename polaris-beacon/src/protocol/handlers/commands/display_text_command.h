#ifndef DISPLAY_TEXT_COMMAND_H
#define DISPLAY_TEXT_COMMAND_H

#include <ArduinoJson.h>

#include <string>

#include "icommand.h"
#include "utils/display_controller.h"

/**
 * @class DisplayTextCommand
 * @brief A command to show a message on the OLED display.
 */
class DisplayTextCommand : public ICommand {
public:
    /**
     * @brief Constructs the DisplayTextCommand.
     * @param displayController Reference to the hardware controller for the display.
     * @param params A JsonObject containing `text`, `size`, and `centered` parameters.
     */
    DisplayTextCommand(DisplayController& displayController, const JsonObject& params);

    /**
     * @brief Executes the command, telling the DisplayController to show the message.
     */
    void execute() override;

private:
    /// @brief Reference to the display hardware controller.
    DisplayController& _displayController;

    /// @brief The message to be displayed.
    std::string _text;

    /// @brief The font size multiplier for the text.
    uint8_t _size;

    /// @brief Whether the text should be centered on the screen.
    bool _centered;
};

#endif  // DISPLAY_TEXT_COMMAND_H