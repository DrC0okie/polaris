#ifndef BEACON_COUNTER_H
#define BEACON_COUNTER_H

#include <Preferences.h>
#include <Ticker.h>

class BeaconCounter {
public:
    // Constructor with optional NVS namespace
    BeaconCounter(const char* nvsKeyName = "counter");

    void begin(Preferences& prefs);

    // Callback
    typedef void (*IncrementCallback_t)(void*);

    uint64_t getValue() const;  // Read current counter
    void reset();               // Optional: reset counter to 0

    void setIncrementCallback(IncrementCallback_t callback, void* context);

private:
    void increment();  // Called every minute
    void save();       // Save to NVS

    static void onTickStatic();      // Ticker-compatible static callback
    static BeaconCounter* instance;  // Static instance pointer

    Ticker _ticker;
    Preferences _prefs;
    const char* _nvsKeyName;
    uint64_t _counter;

    IncrementCallback_t _incrementCallback = nullptr;
    void* _callbackContext = nullptr;
};

#endif  // BEACON_COUNTER_H
