#ifndef WRITE_CHARACTERISTIC_H
#define WRITE_CHARACTERISTIC_H

#include <BLECharacteristic.h>
#include <BLEService.h>
#include <BLEUUID.h>

#include <functional>  // For std::function
#include <memory>      // For std::unique_ptr
#include <string>

#include "icharacteristic.h"

/**
 * @class WriteCharacteristic
 * @brief A concrete implementation of ICharacteristic for a characteristic with WRITE properties.
 */
class WriteCharacteristic : public ICharacteristic {
public:
    /**
     * @brief A function type for the callback executed when a client writes to this characteristic.
     */
    using WriteCallback = std::function<void(const uint8_t* data, size_t len)>;

    /**
     * @brief Constructs a WriteCharacteristic.
     * @param uuid The string representation of the characteristic UUID.
     * @param onWriteAction The callback function to execute on a write event.
     * @param description A human-readable description for the characteristic.
     */
    WriteCharacteristic(const char* uuid, WriteCallback onWriteAction,
                        const std::string& description);

    // See ICharacteristic for documentation of overridden methods.
    bool configure(BLEService& service) override;
    BLECharacteristic* getRawCharacteristic() override;
    std::string getName() const override;
    const BLEUUID& getUUID() const override;

private:
    /**
     * @class CharacteristicWriteHandler
     * @brief An inner class that bridges the BLE library C-style callbacks to the `std::function`
     * used by this class.
     */
    class CharacteristicWriteHandler : public BLECharacteristicCallbacks {
    public:
        explicit CharacteristicWriteHandler(WriteCallback onWriteAction);
        void onWrite(BLECharacteristic* pChar) override;

    private:
        WriteCallback _onWriteAction;
    };

    /// @brief The UUID of this characteristic.
    BLEUUID _uuid;

    /// @brief The modern C++ callback function to be executed on write events.
    WriteCallback _onWriteAction;

    /// @brief A unique pointer to the inner class that handles the raw BLE library callback.
    std::unique_ptr<CharacteristicWriteHandler> _bleCallbackHandler;

    /// @brief A pointer to the underlying library characteristic object.
    BLECharacteristic* _pCharacteristic = nullptr;

    /// @brief The human-readable description of this characteristic.
    std::string _userDescription;
};

#endif  // WRITE_CHARACTERISTIC_H