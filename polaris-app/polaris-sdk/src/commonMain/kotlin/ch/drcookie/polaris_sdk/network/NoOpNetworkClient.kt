package ch.drcookie.polaris_sdk.network

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.ble.model.Beacon
import ch.drcookie.polaris_sdk.ble.model.DeliveryAck
import ch.drcookie.polaris_sdk.ble.model.EncryptedPayload
import ch.drcookie.polaris_sdk.protocol.model.PoLToken

/**
 * A "No-Operation" implementation of the [NetworkClient] interface.
 *
 * Used as a fallback when the developer does not configure an `api` block during SDK initialization.
 */
internal class NoOpNetworkClient : NetworkClient {

    override val knownBeacons: List<Beacon> = emptyList()
    override fun getPhoneId(): Long = -1L

    private fun notConfiguredError() = SdkResult.Failure(
        SdkError.PreconditionError("Network client is not configured. Did you forget to add an api { ... } block during Polaris.initialize?")
    )

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun registerPhone(
        publicKey: UByteArray,
        deviceModel: String,
        osVersion: String,
        appVersion: String,
    ): SdkResult<List<Beacon>, SdkError> = notConfiguredError()

    override suspend fun fetchBeacons(): SdkResult<List<Beacon>, SdkError> = notConfiguredError()
    override suspend fun submitPoLToken(token: PoLToken): SdkResult<Unit, SdkError> = notConfiguredError()
    override suspend fun submitSecureAck(ack: DeliveryAck): SdkResult<Unit, SdkError> = notConfiguredError()
    override suspend fun getPayloadsForDelivery(): SdkResult<List<EncryptedPayload>, SdkError> = notConfiguredError()
    override suspend fun forwardBeaconPayload(beaconId: UInt, payload: ByteArray): SdkResult<ByteArray, SdkError> = notConfiguredError()
    override fun closeClient() {}
}