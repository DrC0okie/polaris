#include "beacon_advertiser.h"

#include <BLEAdvertising.h>
#include <HardwareSerial.h>

const uint8_t EXTENDED_BROADCAST_ADV_INSTANCE = 1;

BeaconAdvertiser::BeaconAdvertiser(uint32_t beaconId, const CryptoService& cryptoService,
                                   MinuteCounter& counter, BLEMultiAdvertising& advertiser)
    : _beaconId(beaconId),
      _cryptoService(cryptoService),
      _counterRef(counter),
      _advertiserRef(advertiser) {
}

void BeaconAdvertiser::begin() {
    Serial.println("[BeaconAdv] Beginning initial advertisement setup.");

    _counterRef.setIncrementCallback(BeaconAdvertiser::onCounterIncremented, this);

    // Perform an initial update to set the first advertisement
    updateAdvertisement();
}

void BeaconAdvertiser::onCounterIncremented(void* context) {
    if (context) {
        static_cast<BeaconAdvertiser*>(context)->handleCounterIncrement();
    }
}

void BeaconAdvertiser::handleCounterIncrement() {
    updateAdvertisement();
}

void BeaconAdvertiser::updateAdvertisement() {
    uint64_t currentCounter = _counterRef.getValue();
    BroadcastPayload payloadContent;
    payloadContent.beaconId = _beaconId;
    payloadContent.counter = currentCounter;

    // Sign beaconId and counter
    _cryptoService.signBeaconBroadcast(payloadContent.signature, _beaconId, currentCounter);

    // Construct the actual advertising data (e.g., Manufacturer Specific Data)
    // Format: [Len1][Type1][ManufID_LSB][ManufID_MSB][BroadcastPayload_Bytes]
    const uint16_t manufacturerId = 0xABCD;                           // company ID
    const size_t dataLenForAdv = 2 + BroadcastPayload::packedSize();  // ManufID + Payload

    uint8_t rawAdvPayload[1 + 1 + dataLenForAdv];  // Full AD Structure: Len + Type + Data
    size_t idx = 0;

    rawAdvPayload[idx++] = 1 + dataLenForAdv;  // Length of this AD structure field (Type + Data)
    rawAdvPayload[idx++] = ESP_BLE_AD_MANUFACTURER_SPECIFIC_TYPE;  // Type
    rawAdvPayload[idx++] = (uint8_t)(manufacturerId & 0xFF);       // Manuf ID LSB
    rawAdvPayload[idx++] = (uint8_t)(manufacturerId >> 8);         // Manuf ID MSB

    // Copy serialized payload content
    memcpy(&rawAdvPayload[idx], &payloadContent, BroadcastPayload::packedSize());
    idx += BroadcastPayload::packedSize();

    // Update the advertising data for the extended instance
    if (!_advertiserRef.setAdvertisingData(EXTENDED_BROADCAST_ADV_INSTANCE, idx, rawAdvPayload)) {
        Serial.println("[BeaconAdv] Failed to update extended advertising data.");
    } else {
        Serial.printf("[BeaconAdv] Extended advertisement updated. Counter: %llu\n",
                      currentCounter);
    }
}