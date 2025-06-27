#include "system_monitor.h"

#include <esp_chip_info.h>
#include <esp_heap_caps.h>

void SystemMonitor::getStatus(JsonObject& statusObject) {
    // Get Free Heap Memory
    statusObject["free_heap"] = heap_caps_get_free_size(MALLOC_CAP_DEFAULT);

    // Get Uptime
    statusObject["uptime_s"] = esp_timer_get_time() / 1000000;

    // Get revision Information
    esp_chip_info_t chip_info;
    esp_chip_info(&chip_info);
    statusObject["chip_rev"] = chip_info.revision;

    Serial.printf("%s: Gathered system status.\n", TAG);
}