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
    _logBuffer.clear();
    _display.clearDisplay();
    _display.display();
}

void DisplayController::showCenteredMessage(const std::string& message, uint8_t size) {
    if (!_isInitialized) return;

    _logBuffer.clear();
    _display.clearDisplay();
    _display.setTextSize(size);
    _display.setTextColor(SSD1306_WHITE);
    _display.setTextWrap(true);

    int16_t x1, y1;
    uint16_t w, h;
    _display.getTextBounds(message.c_str(), 0, 0, &x1, &y1, &w, &h);
    int16_t cursorX = (SCREEN_WIDTH - w) / 2;
    int16_t cursorY = (SCREEN_HEIGHT - h) / 2;
    _display.setCursor(cursorX < 0 ? 0 : cursorX, cursorY < 0 ? 0 : cursorY);
    
    _display.println(message.c_str());
    _display.display();
}

void DisplayController::addLog(const std::string& logMessage) {
    if (!_isInitialized) return;

    // Adds the new message to the buffer
    _logBuffer.push_back(logMessage);

    // buffer rotation with the new data
    if (_logBuffer.size() > MAX_LOG_LINES) {
        _logBuffer.erase(_logBuffer.begin());
    }

    redrawLog();
}

void DisplayController::redrawLog() {
    if (!_isInitialized) return;

    _display.clearDisplay();
    _display.setTextSize(1, 2);
    _display.setTextColor(SSD1306_WHITE);
    _display.setTextWrap(false);


    const int16_t startX = 0;
    const int16_t startY = 3;
    const int16_t lineHeight = 16;
    int16_t currentY = startY;

    for (const auto& line : _logBuffer) {
        _display.setCursor(startX, currentY);
        _display.print(line.c_str());
        
        currentY += lineHeight;

        if (currentY > SCREEN_HEIGHT) {
            break;
        }
    }
    _display.display();
}