#ifndef SYSTEM_EVENT_NOTIFIER_H
#define SYSTEM_EVENT_NOTIFIER_H

#include <functional>
#include <vector>
#include "system_event_type.h"

/*Callback for the notifier*/
using EventListener = std::function<void(SystemEventType)>;

class SystemEventNotifier {
public:
    /**
     * @brief Allows a component to register itself to the system events.
     * @param listener The function to call when an event occurs.
     */
    void registerListener(EventListener listener);

    /**
     * @brief Allows a component to notify the system when an event occurs.
     * @param event The event type
     */
    void notify(SystemEventType event) const;

private:
    std::vector<EventListener> _listeners;
};

#endif // SYSTEM_EVENT_NOTIFIER_H