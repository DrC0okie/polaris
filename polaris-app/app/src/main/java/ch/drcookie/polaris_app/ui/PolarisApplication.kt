package ch.drcookie.polaris_app.ui

import android.app.Application
import ch.drcookie.polaris_app.data.datasource.ble.BleDataSourceImpl
import ch.drcookie.polaris_app.data.datasource.remote.RemoteDataSource

class PolarisApplication : Application() {

    val bleDataSource: BleDataSourceImpl by lazy {
        BleDataSourceImpl(applicationContext)
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