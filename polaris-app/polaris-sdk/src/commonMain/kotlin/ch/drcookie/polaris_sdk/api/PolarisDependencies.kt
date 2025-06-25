package ch.drcookie.polaris_sdk.api

import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.storage.KeyStore
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler

/**
 * Defines the public, read-only API surface of the Polaris SDK.
 */
public interface PolarisDependencies {
    public val apiClient: ApiClient
    public val keyStore: KeyStore
    public val protocolHandler: ProtocolHandler
    public val bleController: BleController
}