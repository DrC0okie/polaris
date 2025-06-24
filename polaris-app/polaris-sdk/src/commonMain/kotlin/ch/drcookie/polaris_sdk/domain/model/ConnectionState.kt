package ch.drcookie.polaris_sdk.domain.model

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    data class Connecting(val deviceAddress: String) : ConnectionState()
    data class Ready(val deviceAddress: String) : ConnectionState()
    data class Failed(val error: String) : ConnectionState()
}