package ch.drcookie.polaris_app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("polaris_prefs", Context.MODE_PRIVATE)

    var apiKey: String?
        get() = prefs.getString("API_KEY", null)
        set(value) = prefs.edit { putString("API_KEY", value) }

    var phoneId: Long
        get() = prefs.getLong("PHONE_ID", -1L)
        set(value) = prefs.edit { putLong("PHONE_ID", value) }
}