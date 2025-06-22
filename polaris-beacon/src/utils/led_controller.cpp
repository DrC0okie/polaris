#include "led_controller.h"

#include <HardwareSerial.h>

// Initialize static instance pointer
LedController* LedController::instance = nullptr;

LedController::LedController() : _pixels(NUM_NEOPIXELS, PIN_NEOPIXEL, NEO_GRB + NEO_KHZ800) {
    instance = this;
}

void LedController::begin() {
    pinMode(NEOPIXEL_POWER, OUTPUT);
    digitalWrite(NEOPIXEL_POWER, HIGH);

    _pixels.begin();
    _pixels.setBrightness(80);
    _pixels.clear();
    _pixels.show();
    Serial.printf("%s Initialized.", TAG);
}

void LedController::startBlinking(float frequency, uint32_t color) {
    stopBlinking();  // Always stop any previous ticker before starting a new one.

    if (frequency <= 0) {
        Serial.printf("%s Frequency is zero or negative. Stopping blink.", TAG);
        return;
    }
    _isBlinking = true;
    _blinkColor = color;
    _ledState = false;  // Start with the LED off, so the first tick turns it on.

    uint32_t period_ms = 500 / frequency;
    _ticker.attach_ms(period_ms, onTickStatic);

    Serial.printf("%s Started blinking at %.2f Hz with color 0x%06X.\n", TAG, frequency, color);
}

void LedController::stopBlinking() {
    if (_isBlinking) {
        _ticker.detach();
        _isBlinking = false;
        _pixels.clear();
        _pixels.show();
    }
}

void LedController::onTickStatic() {
    if (instance) {
        instance->handleTick();
    }
}

void LedController::handleTick() {
    if (!_isBlinking)
        return;

    _ledState = !_ledState;

    if (_ledState) {
        _pixels.setPixelColor(0, _blinkColor);
    } else {
        _pixels.clear();
    }
    _pixels.show();
}