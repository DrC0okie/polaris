#ifndef SYSTEM_MONITOR_H
#define SYSTEM_MONITOR_H

#include <ArduinoJson.h>
#include <stdint.h>

/**
 * @class SystemMonitor
 * @brief A utility for gathering and reporting beacon system status.
 *
 * This class provides methods to collect system metrics
 */
class SystemMonitor {
public:
    /**
     * @brief Populates a JSON object with the current system status.
     * @param statusObject A reference to a `JsonObject` which will be populated
     *                     with key-value pairs representing the system status.
     */
    void getStatus(JsonObject& statusObject);

private:
    /// @brief A tag used for logging from this class.
    static constexpr const char* TAG = "[SysMonitor]";
};

#endif  // SYSTEM_MONITOR_H