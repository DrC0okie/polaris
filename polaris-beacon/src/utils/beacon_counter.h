#ifndef BEACON_COUNTER_H
#define BEACON_COUNTER_H

#include <Preferences.h>
#include <Ticker.h>

/**
 * @class BeaconCounter
 * @brief Manages a minute-based counter that persists across reboots.
 *
 */
class BeaconCounter {
public:
    /**
     * @brief Constructs a BeaconCounter.
     * @param nvsKeyName The key name to use for storing the counter value in NVS.
     */
    BeaconCounter(const char* nvsKeyName = "counter");

    /**
     * @brief Initializes the counter, loads its value from NVS, and starts the timer.
     *
     * This method must be called once during setup.
     *
     * @param prefs A reference to an initialized and opened Preferences object.
     */
    void begin(Preferences& prefs);

    // Callback
    typedef void (*IncrementCallback_t)(void*);

    /**
     * @brief Gets the current value of the counter.
     * @return The current 64-bit counter value.
     */
    uint64_t getValue() const;

    /**
     * @brief Resets the counter to zero.
     */
    void reset();

    /**
     * @brief Registers a callback function to be called when the counter increments.
     *
     * @param callback The function to be called.
     * @param context A void pointer to user-defined context, passed to the callback.
     */
    void setIncrementCallback(IncrementCallback_t callback, void* context);

private:
    /// @brief Increments the counter, saves it, and triggers the callback.
    void increment();

    /// @brief Saves the current counter value to NVS.
    void save();

    /// @brief Static trampoline function for the Ticker callback.
    static void onTickStatic();

    /// @brief Pointer to the singleton instance of this class.
    static BeaconCounter* instance;

    /// @brief The Ticker object that drives the one-minute increment.
    Ticker _ticker;

    /// @brief A reference to the NVS storage object.
    Preferences _prefs;

    /// @brief The name of the key used to store the counter in NVS.
    const char* _nvsKeyName;

    /// @brief The current 64-bit counter value.
    uint64_t _counter;

    /// @brief The user-registered callback function.
    IncrementCallback_t _incrementCallback = nullptr;

    /// @brief The context pointer for the user-registered callback.
    void* _callbackContext = nullptr;
};

#endif  // BEACON_COUNTER_H
