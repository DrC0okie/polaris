package ch.drcookie.polaris_app.data.datasource.ble

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    data class Connecting(val deviceAddress: String) : ConnectionState()
    data class Ready(val deviceAddress: String) : ConnectionState() // Services discovered
    data class Failed(val error: String) : ConnectionState()
}