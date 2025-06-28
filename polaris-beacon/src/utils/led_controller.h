#ifndef LED_CONTROLLER_H
#define LED_CONTROLLER_H

#include <Adafruit_NeoPixel.h>
#include <Ticker.h>

#include "protocol/pol_constants.h"

/**
 * @class LedController
 * @brief Manages the on-board NeoPixel for user feedback.
 *
 * This class provides a high-level interface to control the built-in NeoPixel,
 * abstracting the details of the Adafruit_NeoPixel library. It supports starting
 * and stopping a non-blocking blinking effect at a specified frequency and color.
 */
class LedController {
public:
    /**
     * @brief Constructs the LedController.
     */
    LedController();

    /**
     * @brief Initializes the NeoPixel hardware.
     */
    void begin();

    /**
     * @brief Starts a non-blocking blinking effect.
     *
     * If a blink is already in progress, it will be stopped and replaced with the new one.
     * @param frequency The blinking frequency in Hertz (Hz).
     * @param color The 24-bit RGB color of the blink (e.g., 0xFF0000 for red).
     */
    void startBlinking(float frequency, uint32_t color);

    /**
     * @brief Stops any active blinking effect and turns the NeoPixel off.
     */
    void stopBlinking();

private:
    /// @brief Static trampoline function for the Ticker callback.
    static void onTickStatic();

    /// @brief The actual logic executed by the Ticker to toggle the LED state.
    void handleTick();

    /// @brief The underlying NeoPixel driver object from the Adafruit library.
    Adafruit_NeoPixel _pixels;

    /// @brief The Ticker object that drives the blinking effect.
    Ticker _ticker;

    /// @brief Flag to indicate if the blinking ticker is currently active.
    volatile bool _isBlinking = false;

    /// @brief The current on/off state of the LED during a blink cycle.
    volatile bool _ledState = false;

    /// @brief The color to use for the blinking effect.
    volatile uint32_t _blinkColor = 0;

    /// @brief A tag used for logging from this class.
    static constexpr const char* TAG = "[LedController]";

    /// @brief Pointer to the singleton instance of this class for the static callback.
    static LedController* instance;  // Static instance for Ticker callback
};

#endif  // LED_CONTROLLER_H