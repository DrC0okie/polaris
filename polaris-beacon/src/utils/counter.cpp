#include "counter.h"

// Initialize static instance pointer
MinuteCounter* MinuteCounter::instance = nullptr;

MinuteCounter::MinuteCounter(const char* nvsNamespace, const char* key)
    : _nvsNamespace(nvsNamespace), _key(key), _counter(0) {}

void MinuteCounter::begin() {
    _prefs.begin(_nvsNamespace, false);
    _counter = _prefs.getUInt(_key, 0);

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
}

void MinuteCounter::save() {
    _prefs.putUInt(_key, _counter);
}

uint32_t MinuteCounter::getValue() const {
    return _counter;
}

void MinuteCounter::reset() {
    _counter = 0;
    save();
}
