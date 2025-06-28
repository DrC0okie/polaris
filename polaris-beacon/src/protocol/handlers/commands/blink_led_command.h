#ifndef BLINK_LED_COMMAND_H
#define BLINK_LED_COMMAND_H

#include <ArduinoJson.h>

#include "icommand.h"
#include "utils/led_controller.h"

/**
 * @class BlinkLedCommand
 * @brief A command to make the on-board NeoPixel blink.
 */
class BlinkLedCommand : public ICommand {
public:
    /**
     * @brief Constructs the BlinkLedCommand.
     * @param ledController Reference to the hardware controller for the LED.
     * @param params A JsonObject containing `freq` and `color` parameters.
     */
    BlinkLedCommand(LedController& ledController, const JsonObject& params);

    /**
     * @brief Executes the command, telling the LedController to start blinking.
     */
    void execute() override;

private:
    /// @brief Reference to the LED hardware controller.
    LedController& _ledController;

    /// @brief The blinking frequency in Hz.
    float _frequency;

    /// @brief The blinking color as a 24-bit RGB value.
    uint32_t _color;
};

#endif  // BLINK_LED_COMMAND_H