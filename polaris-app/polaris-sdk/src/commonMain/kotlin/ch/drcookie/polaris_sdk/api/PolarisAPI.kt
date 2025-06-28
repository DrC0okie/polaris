package ch.drcookie.polaris_sdk.api

import ch.drcookie.polaris_sdk.network.NetworkClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.storage.KeyStore
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler

/**
 * Defines the public contract for the Polaris SDK, providing access to its core components.
 *
 * The main [Polaris] object implements this interface, acting as the primary access point
 * for all SDK functionality after initialization.
 */
public interface PolarisAPI {

    /**
     * The network client for communicating with a Polaris-compatible backend server.
     * @see NetworkClient
     */
    public val networkClient: NetworkClient

    /**
     * The secure storage handler for cryptographic keys.
     * @see KeyStore
     */
    public val keyStore: KeyStore

    /**
     * The handler for low-level cryptographic protocol logic.
     * @see ProtocolHandler
     */
    public val protocolHandler: ProtocolHandler

    /**
     * The controller for all Bluetooth Low Energy (BLE) operations.
     * @see BleController
     */
    public val bleController: BleController
}