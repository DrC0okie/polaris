package ch.drcookie.polaris_sdk.api.config

import ch.drcookie.polaris_sdk.api.Polaris
import ch.drcookie.polaris_sdk.api.SdkError

/**
 * A builder that provides a Domain-Specific Language (DSL) for configuring the Polaris SDK.
 *
 * An instance of this class is the receiver for the configuration lambda in [Polaris.initialize],
 * allowing for a readable setup process.
 *
 * ### Example:
 * ```
 * Polaris.initialize(context) {
 *     // 'this' is a PolarisConfig instance
 *
 *     ble {
 *         // 'this' is a BleConfig instance
 *         mtu = 256
 *     }
 *
 *     api {
 *         // 'this' is an NetworkConfigBuilder instance
 *         baseUrl = "https://api.example.com"
 *         authMode = AuthMode.None
 *     }
 * }
 * ```
 */
public class PolarisConfig {
    internal var bleConfig: BleConfig = BleConfig()
    internal var networkConfig: NetworkConfig? = null

    /**
     * Configures the BLE behavior of the SDK.
     *
     * @param block A lambda with a [BleConfig] receiver where you can override default BLE settings.
     */
    public fun ble(block: BleConfig.() -> Unit) {
        bleConfig.apply(block)
    }

    /**
     * Configures and enables the network client for all server communication.
     * If this block is omitted in the `initialize` call, all network-related functionality
     * will be disabled and any calls to [Polaris.networkClient] methods will result in a
     * [SdkError.PreconditionError].
     *
     * @param block A lambda with an [NetworkConfigBuilder] receiver where you must set at least the `baseUrl`.
     */
    public fun api(block: NetworkConfigBuilder.() -> Unit) {
        // Create a builder, apply the user's block, and then build the final config.
        val builder = NetworkConfigBuilder().apply(block)
        networkConfig = builder.build()
    }
}