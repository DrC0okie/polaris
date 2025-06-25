package ch.drcookie.polaris_sdk.ble.model

public sealed class ConnectionState {
    public object Disconnected : ConnectionState()
    public object Scanning : ConnectionState()
    public data class Connecting(public val deviceAddress: String) : ConnectionState()
    public data class Ready(public val deviceAddress: String) : ConnectionState()
    public data class Failed(public val error: String) : ConnectionState()
}