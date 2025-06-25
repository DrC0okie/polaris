package ch.drcookie.polaris_sdk.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

internal class SharedPreferencesProvider(context: Context) : SdkPreferences {
    private val prefs: SharedPreferences = context.getSharedPreferences("polaris_prefs", Context.MODE_PRIVATE)

    override var apiKey: String?
        get() = prefs.getString("API_KEY", null)
        set(value) = prefs.edit { putString("API_KEY", value) }

    override var phoneId: Long
        get() = prefs.getLong("PHONE_ID", -1L)
        set(value) = prefs.edit { putLong("PHONE_ID", value) }

    override var phonePublicKey: String?
        get() = prefs.getString("PHONE_PK", null)
        set(value) = prefs.edit { putString ("PHONE_PK", value) }

    override var phoneSecretKey: String?
        get() = prefs.getString("PHONE_SK", null)
        set(value) = prefs.edit { putString("PHONE_SK", value) }
}