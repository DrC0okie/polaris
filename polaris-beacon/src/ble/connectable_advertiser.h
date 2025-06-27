#ifndef CONNECTABLE_ADVERTISER_H
#define CONNECTABLE_ADVERTISER_H

#include <BLEAdvertising.h>

#include "protocol/pol_constants.h"

class BLEMultiAdvertising;

class ConnectableAdvertiser {
public:
    explicit ConnectableAdvertiser(BLEMultiAdvertising& advertiser);
    void setHasDataPending(bool hasData);

private:
    void updateAdvertisement();

    BLEMultiAdvertising& _advertiserRef;
    uint8_t _statusByte = 0x00;

    static constexpr uint8_t STATUS_FLAG_DATA_PENDING = 0b00000001;
};

#endif  // CONNECTABLE_ADVERTISER_H