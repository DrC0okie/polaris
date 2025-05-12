#ifndef BEACON_ADVERTISER_H
#define BEACON_ADVERTISER_H

#include <BLEAdvertising.h>
#include <stdint.h>

#include "../protocol/pol_constants.h"
#include "../utils/counter.h"

class BLEMultiAdvertising;

class BeaconAdvertiser {
public:
    BeaconAdvertiser(uint32_t beacon_id, const uint8_t sk[Ed25519_SK_SIZE], MinuteCounter& counter,
                     BLEMultiAdvertising& advertiser);

    void begin();
    void updateAdvertisement();  // Public method to trigger an update

private:
    // Callback for MinuteCounter
    static void onCounterIncremented(void* context);
    void handleCounterIncrement();

    uint32_t _beacon_id;
    const uint8_t* _sk;  // Store as pointer, ensure lifetime
    const uint8_t* _pk;  // Store as pointer, ensure lifetime
    MinuteCounter& _counterRef;
    BLEMultiAdvertising& _advertiserRef;

    // Structure for the broadcast payload
    struct BroadcastPayload {
        uint32_t beacon_id;
        uint64_t counter;
        uint8_t signature[SIG_SIZE];

        static constexpr size_t packedSize() {
            return sizeof(beacon_id) + sizeof(counter) + SIG_SIZE;
        }
    };
};

#endif  // BEACON_ADVERTISER_H