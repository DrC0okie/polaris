#include <string>
#include <vector>
#include <BLEAdvertising.h> // For esp_ble_gap_ext_adv_params_t etc.

class BLEMultiAdvertising;

class IAdvertisingSet {
public:
    virtual ~IAdvertisingSet() = default;

    // Gets the parameters for this advertising set
    virtual esp_ble_gap_ext_adv_params_t getAdvParams() const = 0;

    // Gets the advertising data (can be empty if not used)
    virtual std::vector<uint8_t> getAdvData() const = 0;

    // Gets the scan response data (can be empty if not used)
    virtual std::vector<uint8_t> getScanRspData() const = 0;

    // Gets the specific random address for this instance
    // Returns empty vector if public or default random address should be used.
    virtual std::vector<uint8_t> getInstanceAddress() const = 0; // 6 bytes or empty

    // Gets the duration and max_events for this set
    virtual void getDuration(int& duration, int& max_events) const = 0;

    // Method to update dynamic data (if applicable for this set)
    // Called by BleServer when it's time to refresh, or by an external manager.
    // The BLEMultiAdvertising pointer is passed so the set can update itself.
    virtual bool updateDynamicData(BLEMultiAdvertising* advertiser, uint8_t instance_id) {
        (void)advertiser; (void)instance_id; // Suppress unused
        return true; // Default: no dynamic data to update
    }

    virtual uint8_t getAssignedInstanceId() const = 0;
    virtual void setAssignedInstanceId(uint8_t id) = 0;
};