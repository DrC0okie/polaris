package ch.drcookie.polaris_sdk.ble.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import io.github.oshai.kotlinlogging.KotlinLogging

private val Log = KotlinLogging.logger {}

object BleUtils {
    @SuppressLint("MissingPermission")
    fun BluetoothDevice.isConnected(context: Context): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return try {
            bluetoothManager.getConnectionState(this, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED
        } catch (e: SecurityException) {
            Log.error(e) { "Permission missing for getConnectionState" }
            false
        }
    }

    fun gattStatusToString(status: Int): String {
        return when (status) {
            BluetoothGatt.GATT_SUCCESS -> "SUCCESS"
            8 -> "GATT_INSUFFICIENT_ENCRYPTION"
            15 -> "GATT_INSUFFICIENT_AUTHENTICATION"
            133 -> "GATT_ERROR (Timeout or resource issue)"
            257 -> "GATT_INTERNAL_ERROR (Stack issue)"
            5 -> "GATT_INSUFFICIENT_AUTHORIZATION"
            else -> "Unknown GATT Error ($status)"
        }
    }
}