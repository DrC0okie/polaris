#include "connectable_advertiser.h"

#include <BLEDevice.h>
#include <HardwareSerial.h>

#include "ble_manager.h"

ConnectableAdvertiser::ConnectableAdvertiser(BLEMultiAdvertising& advertiser)
    : _advertiserRef(advertiser) {
}

void ConnectableAdvertiser::begin() {
    updateAdvertisementData();
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
        updateAdvertisementData();
    }
}

void ConnectableAdvertiser::updateAdvertisementData() {
    BLEAdvertisementData advData;

    // Set the Flags
    advData.setFlags(ESP_BLE_ADV_FLAG_GEN_DISC | ESP_BLE_ADV_FLAG_BREDR_NOT_SPT);

    // Construct the Manufacturer Data payload
    // New size: 2 bytes (Manuf ID) + 4 bytes (Beacon ID) + 1 byte (Status) = 7 bytes
    uint8_t manufDataPayload[7];
    uint16_t manufId = MANUFACTURER_ID;
    uint32_t beaconId = BEACON_ID;

    // Copy Manuf ID
    memcpy(manufDataPayload, &manufId, sizeof(manufId));
    // Copy Beacon ID
    memcpy(manufDataPayload + sizeof(manufId), &beaconId, sizeof(beaconId));
    // Copy dynamic Status Byte
    memcpy(manufDataPayload + sizeof(manufId) + sizeof(beaconId), &_statusByte,
           sizeof(_statusByte));

    advData.setManufacturerData(
        std::string(reinterpret_cast<const char*>(manufDataPayload), sizeof(manufDataPayload)));

    // Set the Complete Service UUID
    advData.setCompleteServices(BLEUUID(BleManager::POL_SERVICE));

    // Get the final payload and set it
    std::string advPayload = advData.getPayload();

    if (advPayload.length() > ESP_BLE_ADV_DATA_LEN_MAX) {
        Serial.printf(
            "[ConnAdv] WARNING: Constructed advertisement payload too long (%zu bytes, max 31).\n",
            advPayload.length());
    }

    if (!_advertiserRef.setAdvertisingData(LEGACY_TOKEN_ADV_INSTANCE, advPayload.length(),
                                           reinterpret_cast<const uint8_t*>(advPayload.data()))) {
        Serial.println("[ConnAdv] Failed to set legacy advertising data.");
    } else {
        Serial.println("[ConnAdv] Legacy advertising data updated successfully.");
    }
}