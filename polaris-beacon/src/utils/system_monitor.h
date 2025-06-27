#ifndef SYSTEM_MONITOR_H
#define SYSTEM_MONITOR_H

#include <ArduinoJson.h>
#include <stdint.h>

class SystemMonitor {
public:
    void getStatus(JsonObject& statusObject);

private:
    static constexpr const char* TAG = "[SysMonitor]";
};

#endif  // SYSTEM_MONITOR_H