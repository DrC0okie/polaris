package ch.drcookie.polaris_app

import android.bluetooth.le.ScanResult

interface BleListener {
    fun onDebugMessage(message: String)
    fun onMessageReceived(data: ByteArray)
    fun onDeviceNotFound()
    fun onConnectionFailed(status: Int)
    fun onReady() // Signifies services discovered, ready to enable indications
    fun onIndicationEnabled() // Signifies CCCD write success
    fun onBeaconAdvertised(scanResult: ScanResult, detectedBeaconId: UInt?)
}