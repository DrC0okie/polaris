#include "system_event_notifier.h"

void SystemEventNotifier::registerListener(EventListener listener) {
    if (listener) {
        _listeners.push_back(listener);
    }
}

void SystemEventNotifier::notify(SystemEventType event) const {
    for (const auto& listener : _listeners) {
        listener(event);
    }
}