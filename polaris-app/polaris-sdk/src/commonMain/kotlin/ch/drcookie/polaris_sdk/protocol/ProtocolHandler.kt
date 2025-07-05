package ch.drcookie.polaris_sdk.protocol

import ch.drcookie.polaris_sdk.ble.model.Beacon
import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import ch.drcookie.polaris_sdk.protocol.model.PoLRequest
import ch.drcookie.polaris_sdk.protocol.model.PoLResponse

/**
 * Provides an interface for low-level, **synchronous** cryptographic operations related to the Polaris protocol.
 *
 * This component is responsible for the "rules" of the protocol, such as signing data in the correct format and
 * verifying signatures. It is self-contained and performs no I/O.
 *
 * **Note:** While these cryptographic functions are optimized, they are synchronous, CPU-bound operations.
 * For single operations, the execution time is negligible. However, if you plan to call these functions in a tight loop
 * (e.g., verifying thousands of signatures), it is recommended to wrap the loop in a `withContext(Dispatchers.Default)`
 * block to avoid blocking the main thread.
 */
@OptIn(ExperimentalUnsignedTypes::class)
public interface ProtocolHandler {
    /**
     * Signs a [PoLRequest] using the provided secret key.
     *
     * @param request The request object to be signed.
     * @param secretKey The Ed25519 secret key of the phone.
     * @return A new [PoLRequest] instance with the `phoneSig` field populated.
     */
    public fun signPoLRequest(request: PoLRequest, secretKey: UByteArray): PoLRequest

    /**
     * Verifies the signature of a [PoLResponse] from a beacon.
     * This checks that the response's signature is valid and verifies that the nonce matches.
     *
     * @param response The response received from the beacon.
     * @param signedRequest The original, signed request that was sent to the beacon.
     * @param beaconPublicKey The known public key of the beacon that should have signed the response.
     * @return `true` if the signature and nonce are valid, `false` otherwise.
     */
    public fun verifyPoLResponse(response: PoLResponse, signedRequest: PoLRequest, beaconPublicKey: UByteArray): Boolean

    /**
     * Verifies the signature of a broadcast advertisement payload.
     *
     * @param payload The [BroadcastPayload] received from a BLE advertisement.
     * @param beacon The known public key of the beacon that should have signed the payload.
     * @return `true` if the signature is valid, `false` otherwise.
     */
    public fun verifyBroadcast(payload: BroadcastPayload, knownBeacon: Beacon): Boolean

    /**
     * Generates a random nonce to use in protocol messages.
     *
     * @return A [UByteArray] containing the random nonce.
     */
    public fun generateNonce(): UByteArray
}