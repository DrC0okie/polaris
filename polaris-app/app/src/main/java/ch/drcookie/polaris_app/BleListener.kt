package ch.drcookie.polaris_app

interface BleListener {
    fun onDebugMessage(message: String)
    fun onMessageReceived(data: ByteArray)
    fun onDeviceNotFound()
    fun onConnectionFailed(status: Int)
    fun onReady() // Signifies services discovered, ready to enable indications
    fun onIndicationEnabled() // Signifies CCCD write success
}