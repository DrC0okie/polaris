package ch.drcookie.polaris_sdk.api.use_case

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.ble.model.FoundBeacon
import ch.drcookie.polaris_sdk.ble.model.ScanConfig
import ch.drcookie.polaris_sdk.network.NetworkClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.ble.model.Beacon
import ch.drcookie.polaris_sdk.ble.model.ScanMode
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A high-level use case to find a, connectable beacon.
 *
 * Finds the first available beacon from a target list within a specified timeout.
 *
 * @property bleController The controller for all BLE operations.
 * @property networkClient The client used to access the list of `knownBeacons`.
 */
public class ScanForBeacon(
    private val bleController: BleController,
    private val networkClient: NetworkClient,
) {

    /**
     * Executes a scan for a limited time to find the first available beacon from a target list.
     *
     * @param beaconsToFind The list of [Beacon]s to specifically look for.
     * @param timeoutMillis The duration to scan before giving up.
     * @return An [SdkResult] containing a nullable [FoundBeacon]. The value is `null` if no matching beacon was found.
     */
    public suspend operator fun invoke(
        timeoutMillis: Long = 10000L,
        beaconsToFind: List<Beacon> = networkClient.knownBeacons,
    ): SdkResult<FoundBeacon?, SdkError> {

        if (beaconsToFind.isEmpty()) {
            return SdkResult.Success(null)
        }

        val scanConfig = ScanConfig(scanMode = ScanMode.LOW_LATENCY)

        // Call the fallible function and get the result.
        val flowResult = bleController.findConnectableBeacons(scanConfig, beaconsToFind)

        // Check the result. If it's a failure, propagate it.
        val beaconFlow = when (flowResult) {
            is SdkResult.Success -> flowResult.value
            is SdkResult.Failure -> return flowResult
        }

        return runCatching {
            withTimeoutOrNull(timeoutMillis) {
                beaconFlow.firstOrNull()
            }
        }.fold(
            onSuccess = { foundBeacon -> SdkResult.Success(foundBeacon) },
            onFailure = { throwable ->
                SdkResult.Failure(SdkError.BleError("Scan failed during execution: ${throwable.message}"))
            }
        )
    }
}
