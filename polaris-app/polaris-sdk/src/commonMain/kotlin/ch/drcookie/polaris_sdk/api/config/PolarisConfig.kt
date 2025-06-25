package ch.drcookie.polaris_sdk.api.config

/**
 * A builder for configuring the Polaris SDK.
 * Use this in the `Polaris.initialize` block to set custom configurations.
 */
public class PolarisConfig {
    internal var bleConfig: BleConfig = BleConfig()
    internal var apiConfig: ApiConfig? = null

    /**
     * Configures the BLE behavior of the SDK.
     *
     * @param block A lambda where you can set properties of [BleConfig].
     */
    public fun ble(block: BleConfig.() -> Unit) {
        // The user's lambda block is applied to the internal BleConfig instance.
        bleConfig.apply(block)
    }

    /**
     * Configures and enables the network client for server communication.
     *
     * @param block A lambda where you must set the properties of the [ApiConfigBuilder].
     */
    public fun api(block: ApiConfigBuilder.() -> Unit) {
        // Create a builder, apply the user's block, and then build the final config.
        val builder = ApiConfigBuilder().apply(block)
        apiConfig = builder.build()
    }
}