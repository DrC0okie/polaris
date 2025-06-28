package ch.drcookie.polaris_sdk.api.config

import ch.drcookie.polaris_sdk.api.Polaris

/**
 * Configuration for all Bluetooth Low Energy (BLE) interactions.
 *
 * An instance of this class can be modified within the `ble { ... }` block of the
 * [Polaris.initialize] function to adapt the SDK to custom beacon firmware.
 *
 * @property polServiceUuid The GATT service UUID for discovering and interacting with Polaris beacons.
 * @property manufacturerId The manufacturer ID used in BLE advertisements to identify Polaris beacons.
 * @property tokenWriteUuid The characteristic UUID for writing a Proof-of-Location request to the beacon.
 * @property tokenIndicateUuid The characteristic UUID for receiving a PoL response from the beacon.
 * @property encryptedWriteUuid The characteristic UUID for writing a secure, server-originated payload to the beacon.
 * @property encryptedIndicateUuid The characteristic UUID for receiving an ACK from the beacon for a server-originated payload, and for receiving data in the beacon-to-server flow.
 * @property pullDataWriteUuid The characteristic UUID to write to in order to trigger the beacon to send its pending data.
 * @property mtu The Maximum Transmission Unit size to request upon connecting to a beacon. A larger MTU allows for faster data transfer.
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