#include "counter.h"

#include <HardwareSerial.h>

// Initialize static instance pointer
MinuteCounter* MinuteCounter::instance = nullptr;

MinuteCounter::MinuteCounter(Preferences& prefs, const char* key) : _prefs(prefs), _nvsKeyName(key), _counter(0) {
    _counter = _prefs.getULong64(_nvsKeyName, 0);
    Serial.printf("[Counter] Initialized from NVS: %llu\n", _counter);

    // Set static instance pointer so static callback can access 'this'
    instance = this;

    // Ticker needs plain function pointer; use static trampoline
    _ticker.attach(60, MinuteCounter::onTickStatic);
}

void MinuteCounter::onTickStatic() {
    if (instance) {
        instance->increment();
    }
}

void MinuteCounter::increment() {
    _counter++;
    save();

    if (_incrementCallback) {
        _incrementCallback(_callbackContext);
    }
}

void MinuteCounter::save() {
    _prefs.putULong64(_nvsKeyName, _counter);
}

uint64_t MinuteCounter::getValue() const {
    return _counter;
}

void MinuteCounter::reset() {
    _counter = 0;
    save();

    if (_incrementCallback) {
        _incrementCallback(_callbackContext);
    }
}

void MinuteCounter::setIncrementCallback(IncrementCallback_t callback, void* context) {
    _incrementCallback = callback;
    _callbackContext = context;
}
