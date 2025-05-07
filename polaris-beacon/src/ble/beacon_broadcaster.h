#ifndef BEACON_BROADCASTER_H
#define BEACON_BROADCASTER_H

#include <BLEAdvertising.h> // Gives access to BLEMultiAdvertising if BLE5 is enabled
#include <Ticker.h>
#include <stdint.h>
#include <stddef.h>

#include "../protocol/pol_constants.h"
#include "../utils/counter.h"

// Forward declaration might be needed if BLEMultiAdvertising isn't fully defined here
class BLEMultiAdvertising;

class BeaconBroadcaster {
public:
    BeaconBroadcaster(MinuteCounter& counter_ref);
    ~BeaconBroadcaster();

    // Simplified: Focus on Extended Advertising only
    bool begin(uint32_t beacon_id, const uint8_t sk[32], const uint8_t pk[32], uint32_t interval_ms = 5000);
    void stop();

private:
    // Payload size remains the same (id + counter + sig)
    static constexpr size_t BROADCAST_PAYLOAD_SIZE =
        sizeof(uint32_t) + sizeof(uint64_t) + POL_SIG_SIZE;

    static void timer_callback(BeaconBroadcaster* instance);
    // Changed: Update Extended Advertising Data
    void updateAndSetExtAdvData();

    // We don't strictly need BLEMultiAdvertising object if using direct IDF calls,
    // but keep it for now if stop() uses it. Or remove it. Let's keep for stop().
    BLEMultiAdvertising _bleMultiAdv;
    Ticker _updateTimer;
    MinuteCounter& _counter;

    uint32_t _beacon_id = 0;
    uint8_t _sk[32] = {0};
    uint8_t _pk[32] = {0};
    uint8_t _broadcastPayload[BROADCAST_PAYLOAD_SIZE] = {0};

    bool _isRunning = false;
    uint32_t _currentIntervalMs = 0;
};

#endif // BEACON_BROADCASTER_H