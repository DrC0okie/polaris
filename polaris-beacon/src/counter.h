#ifndef COUNTER_H
#define COUNTER_H

#include <Ticker.h>
#include <Preferences.h>

class MinuteCounter {
public:
    // Constructor with optional NVS namespace and key
    MinuteCounter(const char* nvsNamespace = "polaris-beacon", const char* key = "counter");

    void begin();                // Call in setup()
    uint32_t getValue() const;   // Read current counter
    void reset();                // Optional: reset counter to 0

private:
    void increment();            // Called every minute
    void save();                 // Save to NVS

    static void onTickStatic();  // Ticker-compatible static callback
    static MinuteCounter* instance; // Static instance pointer

    Ticker _ticker;
    Preferences _prefs;
    const char* _nvsNamespace;
    const char* _key;
    uint32_t _counter;
};

#endif // COUNTER_H
