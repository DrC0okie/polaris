#ifndef COUNTER_H
#define COUNTER_H

#include <Preferences.h>
#include <Ticker.h>

class MinuteCounter {
public:
    // Callback
    typedef void (*IncrementCallback_t)(void*);

    // Constructor with optional NVS namespace
    MinuteCounter(Preferences& prefs, const char* nvsKeyName = "counter");

    uint64_t getValue() const;  // Read current counter
    void reset();               // Optional: reset counter to 0

    void setIncrementCallback(IncrementCallback_t callback, void* context);

private:
    void increment();  // Called every minute
    void save();       // Save to NVS

    static void onTickStatic();      // Ticker-compatible static callback
    static MinuteCounter* instance;  // Static instance pointer

    Ticker _ticker;
    Preferences _prefs;
    const char* _nvsKeyName;
    uint64_t _counter;

    IncrementCallback_t _incrementCallback = nullptr;
    void* _callbackContext = nullptr;
};

#endif  // COUNTER_H
