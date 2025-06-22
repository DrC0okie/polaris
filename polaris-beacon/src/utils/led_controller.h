#ifndef LED_CONTROLLER_H
#define LED_CONTROLLER_H

#include <Adafruit_NeoPixel.h>
#include <Ticker.h>

#include "protocol/pol_constants.h"

class LedController {
public:
    LedController();
    void begin();
    void startBlinking(float frequency, uint32_t color);
    void stopBlinking();

private:
    static void onTickStatic();
    void handleTick();

    Adafruit_NeoPixel _pixels;
    Ticker _ticker;
    volatile bool _isBlinking = false;
    volatile bool _ledState = false;
    volatile uint32_t _blinkColor = 0;
    static constexpr const char* TAG = "[LedController]";

    static LedController* instance;  // Static instance for Ticker callback
};

#endif  // LED_CONTROLLER_H