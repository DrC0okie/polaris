#ifndef DISPLAY_CONTROLLER_H
#define DISPLAY_CONTROLLER_H

#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

#include <string>

class DisplayController {
public:
    DisplayController();

    // Initializes the I2C connection and the display.
    // Returns true on success, false on failure.
    bool begin();

    // Clears the entire display buffer.
    void clear();

    // Displays a multi-line message. Automatically handles word wrapping.
    void showMessage(const std::string& message, uint8_t size = 1, bool centered = false);

    // Displays the Adafruit splash screen.
    void showSplashScreen();

private:
    static constexpr const char* TAG = "[DisplayCtrl]";
    static constexpr int SCREEN_WIDTH = 128;
    static constexpr int SCREEN_HEIGHT = 64;
    static constexpr int OLED_RESET_PIN = -1;  // No reset pin required for STEMMA QT
    static constexpr uint8_t SCREEN_ADDRESS = 0x3D;

    Adafruit_SSD1306 _display;
    bool _isInitialized = false;
};

#endif  // DISPLAY_CONTROLLER_H