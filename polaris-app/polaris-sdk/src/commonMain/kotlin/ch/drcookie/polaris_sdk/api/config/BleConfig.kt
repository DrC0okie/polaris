package ch.drcookie.polaris_sdk.api.config

/**
 * Configuration for Bluetooth Low Energy interactions.
 *
 * @property polServiceUuid The primary service UUID for discovering connectable Polaris beacons.
 * @property manufacturerId The manufacturer ID used in advertisements.
 * @property tokenWriteUuid The characteristic UUID for writing a PoL request.
 * @property tokenIndicateUuid The characteristic UUID for receiving a PoL response.
 * @property encryptedWriteUuid The characteristic UUID for writing a secure payload.
 * @property encryptedIndicateUuid The characteristic UUID for receiving a secure payload acknowledgement.
 * @property mtu The MTU to negotiate with the beacon.
 */
public data class BleConfig(
    var polServiceUuid: String = "f44dce36-ffb2-565b-8494-25fa5a7a7cd6",
    var manufacturerId: Int = 0xFFFF,
    var tokenWriteUuid: String = "8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b",
    var tokenIndicateUuid: String = "d234a7d8-ea1f-5299-8221-9cf2f942d3df",
    var encryptedWriteUuid: String = "8ed72380-5adb-4d2d-81fb-ae6610122ee8",
    var encryptedIndicateUuid: String = "079b34dd-2310-4b61-89bb-494cc67e097f",
    var pullDataWriteUuid: String = "e914a8e4-843a-4b72-8f2a-f9175d71cf88",
    var mtu: Int = 517
)