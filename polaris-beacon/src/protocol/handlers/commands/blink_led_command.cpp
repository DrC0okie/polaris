#include "blink_led_command.h"
#include <HardwareSerial.h>

BlinkLedCommand::BlinkLedCommand(LedController& ledController, const JsonObject& params)
    : _ledController(ledController) {
    // Handle missing parameters by using defaults.
    _frequency = params["freq"] | 2.0f; // Default to 2 Hz
    _color = params["color"] | 0x0000FF;  // Default to blue
    Serial.printf("[Command] BlinkLed parsed params: Freq=%.2f, Color=0x%06X\n", _frequency, _color);
}

void BlinkLedCommand::execute() {
    Serial.println("[Command] Executing BLINK_LED.");
    _ledController.startBlinking(_frequency, _color);
}