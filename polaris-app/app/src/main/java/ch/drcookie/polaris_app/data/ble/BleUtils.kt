package ch.drcookie.polaris_app.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log

object BleUtils {
    val TAG = BleUtils::class.simpleName

    @SuppressLint("MissingPermission")
    fun BluetoothDevice.isConnected(context: Context): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return try {
            bluetoothManager.getConnectionState(this, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission missing for getConnectionState", e)
            false
        }
    }
}