#include "beacon_counter.h"

#include <HardwareSerial.h>

// Initialize static instance pointer
BeaconCounter* BeaconCounter::instance = nullptr;

BeaconCounter::BeaconCounter(const char* key) : _nvsKeyName(key), _counter(0) {
}

void BeaconCounter::begin(Preferences& prefs) {
    _prefs = prefs;
    _counter = _prefs.getULong64(_nvsKeyName, 0);
    Serial.printf("[Counter] Initialized from NVS: %llu\n", _counter);

    // Set static instance pointer so static callback can access 'this'
    instance = this;

    // Ticker needs plain function pointer; use static trampoline
    _ticker.attach(60, BeaconCounter::onTickStatic);
}

void BeaconCounter::onTickStatic() {
    if (instance) {
        instance->increment();
    }
}

void BeaconCounter::increment() {
    _counter++;
    save();

    if (_incrementCallback) {
        _incrementCallback(_callbackContext);
    }
}

void BeaconCounter::save() {
    _prefs.putULong64(_nvsKeyName, _counter);
}

uint64_t BeaconCounter::getValue() const {
    return _counter;
}

void BeaconCounter::reset() {
    _counter = 0;
    save();

    if (_incrementCallback) {
        _incrementCallback(_callbackContext);
    }
}

void BeaconCounter::setIncrementCallback(IncrementCallback_t callback, void* context) {
    _incrementCallback = callback;
    _callbackContext = context;
}
