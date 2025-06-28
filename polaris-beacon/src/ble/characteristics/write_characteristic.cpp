// write_characteristic.cpp
#include "write_characteristic.h"

#include <HardwareSerial.h>

WriteCharacteristic::CharacteristicWriteHandler::CharacteristicWriteHandler(
    WriteCallback onWriteAction)
    : _onWriteAction(onWriteAction) {
}

void WriteCharacteristic::CharacteristicWriteHandler::onWrite(BLECharacteristic* pChar) {
    if (!pChar)
        return;
    std::string value = pChar->getValue();
    if (_onWriteAction && !value.empty()) {
        _onWriteAction(reinterpret_cast<const uint8_t*>(value.data()), value.size());
    }
}

WriteCharacteristic::WriteCharacteristic(const char* uuid, WriteCallback onWriteAction,
                                         const std::string& description)
    : _uuid(uuid), _onWriteAction(onWriteAction), _userDescription(description) {
    _bleCallbackHandler =
        std::unique_ptr<CharacteristicWriteHandler>(new CharacteristicWriteHandler(_onWriteAction));
}

bool WriteCharacteristic::configure(BLEService& service) {
    _pCharacteristic = service.createCharacteristic(_uuid, BLECharacteristic::PROPERTY_WRITE);
    if (!_pCharacteristic) {
        Serial.printf("[WriteChar] Failed to create characteristic with UUID: %s\n",
                      _uuid.toString().c_str());
        return false;
    }
    _pCharacteristic->setAccessPermissions(ESP_GATT_PERM_WRITE);

    if (!_bleCallbackHandler) {
        Serial.printf("[WriteChar] BLE Callback Handler not initialized for UUID: %s\n",
                      _uuid.toString().c_str());
        return false;
    }
    _pCharacteristic->setCallbacks(_bleCallbackHandler.get());

    if (!_userDescription.empty()) {
        BLEDescriptor* desc = new BLEDescriptor(BLEUUID((uint16_t)0x2901));
        if (desc) {
            desc->setValue(_userDescription);
            desc->setAccessPermissions(ESP_GATT_PERM_READ);
            _pCharacteristic->addDescriptor(desc);
        } else {
            Serial.println("[WriteChar] Failed to allocate User Description descriptor!");
        }
    }
    return true;
}

BLECharacteristic* WriteCharacteristic::getRawCharacteristic() {
    return _pCharacteristic;
}

std::string WriteCharacteristic::getName() const {
    return _userDescription;
}

const BLEUUID& WriteCharacteristic::getUUID() const {
    return _uuid;
}