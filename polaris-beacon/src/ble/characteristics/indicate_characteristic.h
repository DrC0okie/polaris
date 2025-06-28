// indicate_characteristic.h (Simplified)
#ifndef INDICATE_CHARACTERISTIC_H
#define INDICATE_CHARACTERISTIC_H

#include <BLE2902.h>  // For CCCD
#include <BLECharacteristic.h>
#include <BLEService.h>
#include <BLEUUID.h>

#include <string>

#include "icharacteristic.h"

/**
 * @class IndicateCharacteristic
 * @brief Implementation of ICharacteristic for a characteristic with INDICATE properties.
 *
 * This class encapsulates the creation of a characteristic that can send data to a client. It
 * automatically adds the CCCD.
 */
class IndicateCharacteristic : public ICharacteristic {
public:
    /**
     * @brief Constructs an IndicateCharacteristic.
     * @param uuid The string representation of the characteristic UUID.
     * @param description A description for the characteristic.
     */
    IndicateCharacteristic(const char* uuid, const std::string& description);

    // See ICharacteristic for documentation of overridden methods.
    bool configure(BLEService& service) override;
    BLECharacteristic* getRawCharacteristic() override;
    std::string getName() const override;
    const BLEUUID& getUUID() const override;

    /**
     * @brief Sets the value of the characteristic in the local buffer.
     * @param data Pointer to the data buffer.
     * @param len The length of the data.
     */
    void setValue(uint8_t* data, size_t len);

    /**
     * @brief Sends an indication of the current value to a connected client.
     */
    void indicate();

private:
    /// @brief The UUID of this characteristic.
    BLEUUID _uuid;

    /// @brief A pointer to the underlying librar characteristic object.
    BLECharacteristic* _pCharacteristic = nullptr;

    /// @brief The human-readable description of this characteristic.
    std::string _userDescription;
};

#endif  // INDICATE_CHARACTERISTIC_H