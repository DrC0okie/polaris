// indicate_characteristic.h (Simplified)
#ifndef INDICATE_CHARACTERISTIC_H
#define INDICATE_CHARACTERISTIC_H

#include <BLE2902.h>  // For CCCD
#include <BLECharacteristic.h>
#include <BLEService.h>
#include <BLEUUID.h>

#include <string>

#include "icharacteristic.h"

class IndicateCharacteristic : public ICharacteristic {
public:
    IndicateCharacteristic(const char* uuid, const std::string& description);

    bool configure(BLEService& service) override;
    BLECharacteristic* getRawCharacteristic() override;
    std::string getName() const override;
    const BLEUUID& getUUID() const override;

    // Methods specific to indicate/notify
    void setValue(uint8_t* data, size_t len);
    void indicate();

private:
    BLEUUID _uuid;
    BLECharacteristic* _pCharacteristic = nullptr;
    std::string _userDescription;
};

#endif  // INDICATE_CHARACTERISTIC_H