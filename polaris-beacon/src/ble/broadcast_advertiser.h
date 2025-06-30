#ifndef BROADCAST_ADVERTISER_H
#define BROADCAST_ADVERTISER_H

#include <BLEAdvertising.h>
#include <stdint.h>

#include "../protocol/pol_constants.h"
#include "../utils/beacon_counter.h"
#include "../utils/crypto_service.h"

class BLEMultiAdvertising;

/**
 * @class BroadcastAdvertiser
 * @brief Manages the dynamic payload of the extended (non-connectable) BLE advertisement.
 *
 * This class is responsible for periodically updating the extended advertisement
 * with a signed payload containing the beacon ID and current counter value.
 * It listens for updates from a BeaconCounter to trigger these changes.
 */
class BroadcastAdvertiser {
public:
    /**
     * @brief Constructs the BroadcastAdvertiser.
     * @param beaconId The unique ID of this beacon.
     * @param cryptoService Reference to the service for signing the payload.
     * @param counter Reference to the counter that provides the dynamic value.
     * @param advertiser Reference to the main BLEMultiAdvertising instance.
     */
    BroadcastAdvertiser(uint32_t beaconId, const CryptoService& cryptoService,
                        BeaconCounter& counter, BLEMultiAdvertising& advertiser);

    /**
     * @brief Initializes the advertiser.
     *
     * Sets up the callback with the BeaconCounter and performs the first
     * advertisement update.
     */
    void begin();

    /**
     * @brief Manually triggers an update of the advertisement data.
     */
    void updateAdvertisement();  // Public method to trigger an update

private:
    /**
     * @brief Static trampoline function for the BeaconCounter callback.
     * @param context A void pointer to the BroadcastAdvertiser instance.
     */
    static void onCounterIncremented(void* context);

    /**
     * @brief The instance method called when the counter increments.
     */
    void handleCounterIncrement();

    /// @brief The unique ID of this beacon.
    const uint32_t _beaconId;

    /// @brief A reference to the cryptographic service.
    const CryptoService& _cryptoService;

    /// @brief A reference to the beacon counter.
    BeaconCounter& _counterRef;

    /// @brief A reference to the ESP32 multi-advertising controller.
    BLEMultiAdvertising& _advertiserRef;

    /**
     * @struct BroadcastPayload
     * @brief Defines the structure of the data payload for the extended advertisement.
     */
    struct BroadcastPayload {
        /// @brief The beacon unique ID.
        uint32_t beaconId;

        /// @brief The beacon current counter value.
        uint64_t counter;

        /// @brief The Ed25519 signature of the beaconId and counter.
        uint8_t signature[SIG_SIZE];

        /**
         * @brief The total size of the packed payload structure.
         */
        static constexpr size_t packedSize() {
            return sizeof(beaconId) + sizeof(counter) + SIG_SIZE;
        }
    };
};

#endif  // BROADCAST_ADVERTISER_H