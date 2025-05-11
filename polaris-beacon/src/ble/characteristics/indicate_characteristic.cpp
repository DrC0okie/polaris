#include "indicate_characteristic.h"

#include <HardwareSerial.h>

IndicateCharacteristic::IndicateCharacteristic(const char* uuid, const std::string& description)
    : _uuid(uuid), _userDescription(description) {
}

bool IndicateCharacteristic::configure(BLEService& service) {
    _pCharacteristic = service.createCharacteristic(_uuid, BLECharacteristic::PROPERTY_INDICATE);
    if (!_pCharacteristic) {
        Serial.printf("[IndicateChar] Failed to create characteristic UUID: %s\n",
                      _uuid.toString().c_str());
        return false;
    }
    _pCharacteristic->setAccessPermissions(ESP_GATT_PERM_READ);

    BLE2902* cccd = new BLE2902();
    if (!cccd) {
        Serial.println("[IndicateChar] Failed to allocate BLE2902 (CCCD)!");
        return false;
    }
    cccd->setAccessPermissions(ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE);
    _pCharacteristic->addDescriptor(cccd);

    if (!_userDescription.empty()) {
        BLEDescriptor* desc = new BLEDescriptor(BLEUUID((uint16_t)0x2901));
        if (desc) {
            desc->setValue(_userDescription);
            desc->setAccessPermissions(ESP_GATT_PERM_READ);
            _pCharacteristic->addDescriptor(desc);
        } else {
            Serial.println("[IndicateChar] Failed to allocate User Description descriptor!");
        }
    }
    return true;
}

BLECharacteristic* IndicateCharacteristic::getRawCharacteristic() {
    return _pCharacteristic;
}

std::string IndicateCharacteristic::getName() const {
    return _userDescription;
}

const BLEUUID& IndicateCharacteristic::getUUID() const {
    return _uuid;
}

void IndicateCharacteristic::setValue(uint8_t* data, size_t len) {
    if (_pCharacteristic) {
        _pCharacteristic->setValue(data, len);
    }
}

void IndicateCharacteristic::indicate() {
    if (_pCharacteristic) {
        _pCharacteristic->indicate();
    }
}