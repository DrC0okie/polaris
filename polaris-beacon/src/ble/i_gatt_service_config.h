
#include <string>
#include <vector>
#include <memory>
#include <BLEServer.h> // For BLEService

#include "i_characteristic_config.h"

class BleServer;
class ICharacteristicConfig;

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