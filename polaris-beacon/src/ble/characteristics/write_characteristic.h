#ifndef WRITE_CHARACTERISTIC_H
#define WRITE_CHARACTERISTIC_H

#include <BLECharacteristic.h>
#include <BLEService.h>
#include <BLEUUID.h>

#include <functional>  // For std::function
#include <memory>      // For std::unique_ptr
#include <string>

#include "icharacteristic.h"

class WriteCharacteristic : public ICharacteristic {
public:
    // Callback called when the characteristic is written by the client
    using WriteCallback = std::function<void(const uint8_t* data, size_t len)>;

    WriteCharacteristic(const char* uuid, WriteCallback onWriteAction,
                        const std::string& description);

    bool configure(BLEService& service) override;
    BLECharacteristic* getRawCharacteristic() override;
    std::string getName() const override;
    const BLEUUID& getUUID() const override;

private:
    // Inner class for BLECharacteristicCallbacks
    class CharacteristicWriteHandler : public BLECharacteristicCallbacks {
    public:
        explicit CharacteristicWriteHandler(WriteCallback onWriteAction);
        void onWrite(BLECharacteristic* pChar) override;

    private:
        WriteCallback _onWriteAction;
    };

    BLEUUID _uuid;
    WriteCallback _onWriteAction;
    std::unique_ptr<CharacteristicWriteHandler> _bleCallbackHandler;
    BLECharacteristic* _pCharacteristic = nullptr;
    std::string _userDescription;
};

#endif  // WRITE_CHARACTERISTIC_H