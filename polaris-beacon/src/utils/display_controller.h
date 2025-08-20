#ifndef DISPLAY_CONTROLLER_H
#define DISPLAY_CONTROLLER_H

#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <vector>
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
     * @brief Displays a simple message.
     * @param The message to display
     * @param size The font size
     */
    void showCenteredMessage(const std::string& message, uint8_t size = 1);

    /**
     * @brief Adds a message to the display that acts as a terminal
     * @param logMessage The message to add
     */
    void addLog(const std::string& logMessage);

    /**
     * @brief Displays the default Adafruit splash screen.
     */
    void showSplashScreen();

private:

    /**
     * @brief Redraws the entire log with updated data
     */
    void redrawLog();

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

    /// @brief Max number of text lines on the display
    static constexpr int MAX_LOG_LINES = 4;
    
    /// @brief Buffer that holds the display messages
    std::vector<std::string> _logBuffer;
};

#endif  // DISPLAY_CONTROLLER_H