
#include <string>
#include <vector>
#include <memory>
#include <BLEServer.h>       // For BLEService, BLECharacteristic
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


class IGattServiceConfig {
public:
    virtual ~IGattServiceConfig() = default;
    virtual BLEUUID getServiceUUID() const = 0;
    // Returns a list of characteristic configurators for this service
    virtual std::vector<std::unique_ptr<ICharacteristicConfig>> getCharacteristicConfigs() = 0;
    // Could add methods for primary/secondary service etc.
    // Callback for when this specific service is started (optional)
    virtual void onServiceStarted(BLEService* pService) { (void)pService; }
};