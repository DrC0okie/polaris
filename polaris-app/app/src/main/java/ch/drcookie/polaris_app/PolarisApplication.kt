package ch.drcookie.polaris_app

import android.app.Application
import ch.drcookie.polaris_app.data.ble.BleDataSource
import ch.drcookie.polaris_app.data.remote.RemoteDataSource

class PolarisApplication : Application() {

    val bleDataSource: BleDataSource by lazy {
        BleDataSource(applicationContext)
    }

    val remoteDataSource: RemoteDataSource by lazy {
        RemoteDataSource()
    }

    override fun onTerminate() {
        super.onTerminate()
        bleDataSource.cancelAll()
        remoteDataSource.shutdown()
    }
}