#ifndef CONNECTABLE_ADVERTISER_H
#define CONNECTABLE_ADVERTISER_H

#include <BLEAdvertising.h>

#include "protocol/pol_constants.h"

class BLEMultiAdvertising;

/**
 * @class ConnectableAdvertiser
 * @brief Manages the dynamic payload of the legacy BLE advertisement.
 *
 * This class is responsible for updating the manufacturer data field of the legacy advertisement to
 * signal if the beacon has data pending for a client.
 */
class ConnectableAdvertiser {
public:
    /**
     * @brief Constructs the ConnectableAdvertiser.
     * @param advertiser A reference to the main BLEMultiAdvertising instance.
     */
    explicit ConnectableAdvertiser(BLEMultiAdvertising& advertiser);

    /**
     * @brief Sets the initial advertising payload.
     *
     * This should be called after the advertising parameters have been configured
     * in the BleManager.
     */
    void begin();

    /**
     * @brief Sets the "data pending" flag in the advertisement payload.
     *
     * It updates the advertisement data only if the new state is different from the current state.
     * @param hasData True to raise the flag, false to lower it.
     */
    void setHasDataPending(bool hasData);

private:
    /**
     * @brief Reconstructs the entire advertisement data payload and sends it to the BLE stack.
     */
    void updateAdvertisementData();

    /// @brief A reference to the ESP32 multi-advertising controller.
    BLEMultiAdvertising& _advertiserRef;

    /// @brief The status byte included in the manufacturer data.
    uint8_t _statusByte = 0x00;

    /// @brief Bitmask for the "data pending" flag within the status byte.
    static constexpr uint8_t STATUS_FLAG_DATA_PENDING = 0b00000001;
};

#endif  // CONNECTABLE_ADVERTISER_H