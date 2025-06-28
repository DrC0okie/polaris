#ifndef DISPLAY_CONTROLLER_H
#define DISPLAY_CONTROLLER_H

#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

#include <string>

/**
 * @class DisplayController
 * @brief Manages all interactions with the SSD1306 OLED display.
 *
 * This class provides a simplified interface for displaying text
 * on the OLED screen. It abstracts the details of the I2C
 * communication and the Adafruit_GFX library calls.
 */
class DisplayController {
public:
    /**
     * @brief Constructs the DisplayController.
     */
    DisplayController();

    /**
     * @brief Initializes the I2C bus and the display hardware.
     * @return True if the display is successfully initialized, false otherwise.
     */
    bool begin();

    /**
     * @brief Clears the display screen.
     *
     * Erases the internal buffer and updates the screen to be blank.
     */
    void clear();

    /**
     * @brief Displays a message on the screen.
     *
     * This method handles clearing the screen, setting text properties,
     * and printing the message with automatic word wrapping.
     *
     * @param message The string message to display. Can include '\n' for new lines.
     * @param size The font size multiplier (e.g., 1 for default, 2 for double size).
     * @param centered If true, attempts to center the text on the screen.
     */
    void showMessage(const std::string& message, uint8_t size = 1, bool centered = false);

    /**
     * @brief Displays the default Adafruit splash screen.
     */
    void showSplashScreen();

private:
    /// @brief A tag used for logging from this class.
    static constexpr const char* TAG = "[DisplayCtrl]";

    /// @brief The width of the OLED screen in pixels.
    static constexpr int SCREEN_WIDTH = 128;

    /// @brief The height of the OLED screen in pixels.
    static constexpr int SCREEN_HEIGHT = 64;

    /// @brief The reset pin for the display (-1 indicates not used).
    static constexpr int OLED_RESET_PIN = -1;

    /// @brief The I2C address of the SSD1306 display.
    static constexpr uint8_t SCREEN_ADDRESS = 0x3D;

    /// @brief The underlying display driver object from the Adafruit library.
    Adafruit_SSD1306 _display;

    /// @brief A flag to indicate if the display has been successfully initialized.
    bool _isInitialized = false;
};

#endif  // DISPLAY_CONTROLLER_H