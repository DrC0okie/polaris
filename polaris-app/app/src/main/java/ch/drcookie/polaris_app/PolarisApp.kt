package ch.drcookie.polaris_app

import android.app.Application
import com.google.crypto.tink.config.TinkConfig

class PolarisApp: Application() {
    override fun onCreate() {
        super.onCreate()
        TinkConfig.register()
    }
}