
#include <BLEServer.h>// For BLECharacteristic
#include <BLEUUID.h>

class BleServer;

// Base for characteristic configuration
class ICharacteristicConfig {
public:
    virtual ~ICharacteristicConfig() = default;
    virtual BLEUUID getUUID() const = 0;
    virtual uint32_t getProperties() const = 0;
    virtual esp_gatt_perm_t getPermissions() const = 0;
    // Could add methods for initial value, descriptors, callbacks etc.
    virtual void configure(BLECharacteristic* pChar, BleServer* pBleServer) = 0;
};