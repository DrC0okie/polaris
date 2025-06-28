package ch.drcookie.polaris_sdk.ble.model

/**
 * Represents the states of a BLE connection to a peripheral device.
 */
public sealed class ConnectionState {

    /** The initial state. No connection is active or pending. */
    public object Disconnected : ConnectionState()

    /** The SDK is actively scanning for nearby devices. */
    public object Scanning : ConnectionState()

    /** A connection attempt to a specific device has been initiated but is not yet complete. */
    public data class Connecting(public val deviceAddress: String) : ConnectionState()

    /** A connection is fully established, and GATT services have been discovered. The device is ready for data exchange. */
    public data class Ready(public val deviceAddress: String) : ConnectionState()

    /** The last connection attempt failed. Contains an error message describing the cause of the failure. */
    public data class Failed(public val error: String) : ConnectionState()
}