package ch.drcookie.polaris_sdk.api.flows

import ch.drcookie.polaris_sdk.ble.model.ConnectionState
import ch.drcookie.polaris_sdk.network.dto.EncryptedPayloadDto
import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import kotlinx.coroutines.flow.first

/**
 * Handles the entire flow of delivering a single secure payload to a beacon.
 *
 * @return The raw ACK/ERR blob received from the beacon, or null if the flow failed
 *         before receiving the blob (e.g., beacon not found).
 */
class DeliverPayloadFlow(
    private val bleController: BleController,
    private val apiClient: ApiClient,
    private val scanForBeacon: ScanForBeaconFlow
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend operator fun invoke(payloadJob: EncryptedPayloadDto): ByteArray? {
        // Find the specific beacon required for this job
        val targetBeaconInfo = apiClient.knownBeacons.find { it.beaconId == payloadJob.beaconId }
        if (targetBeaconInfo == null) {
            // Internal logic error, the server gave us a job for a beacon we don't know.
            throw IllegalStateException("Error: Beacon #${payloadJob.beaconId} is not in the known list.")
        }

        // Use other interactor to find the beacon
        val foundBeacon = scanForBeacon(beaconsToFind = listOf(targetBeaconInfo))

        if (foundBeacon == null) {
            return null
        }

        // Connect, deliver, and disconnect
        try {
            bleController.connect(foundBeacon.address)
            bleController.connectionState.first { it is ConnectionState.Ready }

            val ackBlob = bleController.deliverSecurePayload(payloadJob.encryptedBlob.asByteArray())

            // The interactor's job is to get the ACK. The VM's job is to submit it.
            return ackBlob
        } finally {
            bleController.disconnect()
        }
    }
}