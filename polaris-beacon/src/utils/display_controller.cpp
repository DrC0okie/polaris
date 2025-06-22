#include "display_controller.h"

#include <HardwareSerial.h>
#include <Wire.h>

DisplayController::DisplayController()
    : _display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET_PIN) {
}

bool DisplayController::begin() {
    if (!Wire.begin(41, 40)) {  // SDA=41, SCL=40
        Serial.printf("%s Failed to initialize I2C bus (Wire1).\n", TAG);
        return false;
    }
    Serial.printf("%s I2C bus initialized.\n", TAG);

    delay(250);

    if (!_display.begin(SSD1306_SWITCHCAPVCC, SCREEN_ADDRESS)) {
        Serial.printf("%s SSD1306 allocation failed. Check I2C address and wiring.\n", TAG);
        _isInitialized = false;
        return false;
    }

    _isInitialized = true;
    Serial.printf("%s OLED Display Initialized.\n", TAG);

    _display.clearDisplay();
    _display.display();
    return true;
}

void DisplayController::clear() {
    if (!_isInitialized)
        return;
    _display.clearDisplay();
    _display.display();
}

void DisplayController::showMessage(const std::string& message, uint8_t size, bool centered) {
    if (!_isInitialized)
        return;

    _display.clearDisplay();
    _display.setTextSize(size);
    _display.setTextColor(SSD1306_WHITE);
    _display.setCursor(0, 0);
    _display.setTextWrap(true);  // Enable word wrapping

    if (centered) {
        // Simple centering logic (not perfect, but good for short messages)
        int16_t x1, y1;
        uint16_t w, h;
        _display.getTextBounds(message.c_str(), 0, 0, &x1, &y1, &w, &h);
        int16_t cursorX = (SCREEN_WIDTH - w) / 2;
        int16_t cursorY = (SCREEN_HEIGHT - h) / 2;
        _display.setCursor(cursorX < 0 ? 0 : cursorX, cursorY < 0 ? 0 : cursorY);
    }

    _display.println(message.c_str());
    _display.display();
}

void DisplayController::showSplashScreen() {
    if (!_isInitialized)
        return;
    _display.display();  // The library initializes with the splash screen
}