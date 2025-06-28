// icharacteristic.h
#ifndef ICHARACTERISTIC_H
#define ICHARACTERISTIC_H

#include <BLEService.h>

#include <string>

/**
 * @interface ICharacteristic
 * @brief An interface for a wrapper around a BLE characteristic.
 *
 * Defines a common contract for creating and managing BLE characteristics
 */
class ICharacteristic {
public:
    virtual ~ICharacteristic() = default;

    /**
     * @brief Creates and configures the underlying BLE characteristic within a given service.
     * @param service The BLE service to which this characteristic will be added.
     * @return True on success, false on failure.
     */
    virtual bool configure(BLEService& service) = 0;

    /**
     * @brief Gets a pointer to the raw `BLECharacteristic` object from the BLE library.
     * @return A pointer to the underlying characteristic object.
     */
    virtual BLECharacteristic* getRawCharacteristic() = 0;

    /**
     * @brief Gets the name or description of the characteristic.
     * @return A string containing the characteristic name.
     */
    virtual std::string getName() const = 0;

    /**
     * @brief Gets the UUID of the characteristic.
     * @return A const reference to the `BLEUUID` object.
     */
    virtual const BLEUUID& getUUID() const = 0;
};

#endif  // ICHARACTERISTIC_H