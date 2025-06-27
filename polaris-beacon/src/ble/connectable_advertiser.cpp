#include "connectable_advertiser.h"

#include <BLEDevice.h>
#include <HardwareSerial.h>

#include "ble_manager.h"  // For LEGACY_TOKEN_ADV_INSTANCE and POL_SERVICE

ConnectableAdvertiser::ConnectableAdvertiser(BLEMultiAdvertising& advertiser)
    : _advertiserRef(advertiser) {
}

void ConnectableAdvertiser::setHasDataPending(bool hasData) {
    bool isCurrentlySet = (_statusByte & STATUS_FLAG_DATA_PENDING) != 0;
    bool stateChanged = (hasData != isCurrentlySet);

    if (stateChanged) {
        if (hasData) {
            _statusByte |= STATUS_FLAG_DATA_PENDING;
        } else {
            _statusByte &= ~STATUS_FLAG_DATA_PENDING;
        }
        Serial.printf("[ConnAdv] Data pending flag changed to: %d. Updating advertisement.\n",
                      hasData);
        updateAdvertisement();
    }
}

void ConnectableAdvertiser::updateAdvertisement() {
    BLEAdvertisementData advData;
    advData.setFlags(ESP_BLE_ADV_FLAG_GEN_DISC | ESP_BLE_ADV_FLAG_BREDR_NOT_SPT);
    advData.setCompleteServices(BLEUUID(BleManager::POL_SERVICE));

    // Manufacturer Data: [ManufID(2B)][BeaconID(4B)][Status(1B)]
    uint8_t manufDataPayload[sizeof(uint16_t) + sizeof(uint32_t) + sizeof(uint8_t)];
    size_t offset = 0;

    uint16_t manufId = MANUFACTURER_ID;
    memcpy(manufDataPayload + offset, &manufId, sizeof(manufId));
    offset += sizeof(manufId);

    uint32_t beaconId = BEACON_ID;
    memcpy(manufDataPayload + offset, &beaconId, sizeof(beaconId));
    offset += sizeof(beaconId);

    memcpy(manufDataPayload + offset, &_statusByte, sizeof(_statusByte));

    advData.setManufacturerData(
        std::string(reinterpret_cast<const char*>(manufDataPayload), sizeof(manufDataPayload)));

    std::string advPayload = advData.getPayload();
    if (!_advertiserRef.setAdvertisingData(LEGACY_TOKEN_ADV_INSTANCE, advPayload.length(),
                                           reinterpret_cast<const uint8_t*>(advPayload.data()))) {
        Serial.println("[ConnAdv] Failed to set legacy advertising data.");
    }
}