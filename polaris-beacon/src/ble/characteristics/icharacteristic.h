// icharacteristic.h
#ifndef ICHARACTERISTIC_H
#define ICHARACTERISTIC_H

#include <BLEService.h>

#include <string>

class ICharacteristic {
public:
    virtual ~ICharacteristic() = default;

    // Creates and configures the BLE characteristic within the given service.
    // Returns true on success, false on failure.
    virtual bool configure(BLEService& service) = 0;

    // Gets the underlying BLECharacteristic pointer if needed
    virtual BLECharacteristic* getRawCharacteristic() = 0;

    // Gets the user description of the characteristic
    virtual std::string getName() const = 0;

    // Gets the UUID of the characteristic
    virtual const BLEUUID& getUUID() const = 0;
};

#endif  // ICHARACTERISTIC_H