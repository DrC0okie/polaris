#ifndef BEACON_ADVERTISER_H
#define BEACON_ADVERTISER_H

#include <BLEAdvertising.h>
#include <stdint.h>

#include "../protocol/pol_constants.h"
#include "../utils/beacon_counter.h"
#include "../utils/crypto_service.h"

class BLEMultiAdvertising;

class BeaconAdvertiser {
public:
    BeaconAdvertiser(uint32_t beaconId, const CryptoService& cryptoService, BeaconCounter& counter,
                     BLEMultiAdvertising& advertiser);

    void begin();
    void updateAdvertisement();  // Public method to trigger an update

private:
    // Callback for BeaconCounter
    static void onCounterIncremented(void* context);
    void handleCounterIncrement();

    const uint32_t _beaconId;
    const CryptoService& _cryptoService;
    BeaconCounter& _counterRef;
    BLEMultiAdvertising& _advertiserRef;

    // Structure for the broadcast payload
    struct BroadcastPayload {
        uint32_t beaconId;
        uint64_t counter;
        uint8_t signature[SIG_SIZE];

        static constexpr size_t packedSize() {
            return sizeof(beaconId) + sizeof(counter) + SIG_SIZE;
        }
    };
};

#endif  // BEACON_ADVERTISER_H