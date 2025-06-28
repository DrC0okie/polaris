package ch.drcookie.polaris_sdk.api.flows

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.ble.model.FoundBeacon
import ch.drcookie.polaris_sdk.ble.model.ScanConfig
import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.ble.model.Beacon
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

public class ScanForBeaconFlow(
    private val bleController: BleController,
    private val apiClient: ApiClient,
) {

    /**
     * Scans for a limited time to find the first available beacon from a target list.
     * @param timeoutMillis The duration to scan before giving up.
     * @param beaconsToFind The list of beacons we are interested in. Defaults to all known beacons.
     * @return The [FoundBeacon] if one is found within the timeout, otherwise null.
     */
    public suspend operator fun invoke(
        timeoutMillis: Long = 10000L,
        beaconsToFind: List<Beacon> = apiClient.knownBeacons,
    ): SdkResult<FoundBeacon?, SdkError> {

        if (beaconsToFind.isEmpty()) {
            return SdkResult.Success(null)
        }

        val scanConfig = ScanConfig()

        // Call the fallible function and get the result.
        val flowResult = bleController.findConnectableBeacons(scanConfig, beaconsToFind)

        // Check the result. If it's a failure, propagate it immediately.
        val beaconFlow = when (flowResult) {
            is SdkResult.Success -> flowResult.value
            is SdkResult.Failure -> return flowResult
        }

        // Now that we have a valid flow, use runCatching to handle exceptions that might occur.
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
