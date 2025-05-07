#ifndef BEACON_ADVERTISER_H
#define BEACON_ADVERTISER_H

#include <stdint.h>
#include <BLEAdvertising.h> // For BLEMultiAdvertising (if used directly) or forward declare
#include "../utils/counter.h"     // Assuming MinuteCounter is in utils/
#include "../protocol/pol_constants.h" // For POL_SIG_SIZE

// Forward declaration if BLEMultiAdvertising is not included directly
class BLEMultiAdvertising;

class BeaconAdvertiser {
public:
    BeaconAdvertiser(uint32_t beacon_id,
                     const uint8_t secret_key[32],
                     const uint8_t public_key[32],
                     MinuteCounter& counter,
                     BLEMultiAdvertising& advertiser); // Pass by reference

    void begin(); // Call to set initial advertisement and link counter callback
    void updateAdvertisement(); // Public method to trigger an update

private:
    // Callback for MinuteCounter
    static void onCounterIncremented(void* context);
    void handleCounterIncrement();

    uint32_t _beacon_id;
    const uint8_t* _secret_key; // Store as pointer, ensure lifetime
    const uint8_t* _public_key; // Store as pointer, ensure lifetime
    MinuteCounter& _counterRef;
    BLEMultiAdvertising& _advertiserRef;

    // Structure for the broadcast payload
    struct BroadcastPayload {
        uint32_t beacon_id;
        uint64_t counter;
        uint8_t signature[POL_SIG_SIZE];

        static constexpr size_t packedSize() {
            return sizeof(beacon_id) + sizeof(counter) + POL_SIG_SIZE;
        }
    };
};

#endif // BEACON_ADVERTISER_H