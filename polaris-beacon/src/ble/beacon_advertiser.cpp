#include "beacon_advertiser.h"

#include <BLEAdvertising.h>  // For BLEMultiAdvertising methods, ESP_BLE_AD_MANUFACTURER_SPECIFIC_TYPE
#include <HardwareSerial.h>  // For Serial

#include "../protocol/crypto.h"  // For signBeaconBroadcast

// Defined in ble_server.h or a common header
const uint8_t EXTENDED_ADV_INSTANCE = 1;

BeaconAdvertiser::BeaconAdvertiser(uint32_t beacon_id, const uint8_t sk[POL_Ed25519_SK_SIZE],
                                   MinuteCounter& counter, BLEMultiAdvertising& advertiser)
    : _beacon_id(beacon_id), _sk(sk), _counterRef(counter), _advertiserRef(advertiser) {
}

void BeaconAdvertiser::begin() {
    Serial.println("[BeaconAdv] Beginning initial advertisement setup.");
    // Set the callback in MinuteCounter
    _counterRef.setIncrementCallback(BeaconAdvertiser::onCounterIncremented, this);

    // Perform an initial update to set the first advertisement
    updateAdvertisement();
}

void BeaconAdvertiser::onCounterIncremented(void* context) {
    Serial.println("[BeaconAdv] Counter increment detected.");
    if (context) {
        static_cast<BeaconAdvertiser*>(context)->handleCounterIncrement();
    }
}

void BeaconAdvertiser::handleCounterIncrement() {
    updateAdvertisement();
}

void BeaconAdvertiser::updateAdvertisement() {
    Serial.println("[BeaconAdv] Updating extended advertisement data...");

    uint64_t current_counter = _counterRef.getValue();
    BroadcastPayload payload_content;
    payload_content.beacon_id = _beacon_id;
    payload_content.counter = current_counter;

    // Sign beacon_id and counter
    signBeaconBroadcast(payload_content.signature, _beacon_id, current_counter, _sk);

    // Construct the actual advertising data (e.g., Manufacturer Specific Data)
    // Format: [Len1][Type1][ManufID_LSB][ManufID_MSB][BroadcastPayload_Bytes]
    const uint16_t manufacturer_id = 0xABCD;                             // company ID
    const size_t data_len_for_adv = 2 + BroadcastPayload::packedSize();  // ManufID + Payload

    uint8_t raw_adv_payload[1 + 1 + data_len_for_adv];  // Full AD Structure: Len + Type + Data
    size_t idx = 0;

    raw_adv_payload[idx++] =
        1 + data_len_for_adv;  // Length of this AD structure field (Type + Data)
    raw_adv_payload[idx++] = ESP_BLE_AD_MANUFACTURER_SPECIFIC_TYPE;  // Type
    raw_adv_payload[idx++] = (uint8_t)(manufacturer_id & 0xFF);      // Manuf ID LSB
    raw_adv_payload[idx++] = (uint8_t)(manufacturer_id >> 8);        // Manuf ID MSB

    // Copy serialized payload content
    memcpy(&raw_adv_payload[idx], &payload_content, BroadcastPayload::packedSize());
    idx += BroadcastPayload::packedSize();

    // Update the advertising data for the extended instance
    if (!_advertiserRef.setAdvertisingData(EXTENDED_ADV_INSTANCE, idx, raw_adv_payload)) {
        Serial.println("[BeaconAdv] Failed to update extended advertising data.");
    } else {
        Serial.printf("[BeaconAdv] Extended advertisement updated. Counter: %llu\n",
                      current_counter);
    }
}