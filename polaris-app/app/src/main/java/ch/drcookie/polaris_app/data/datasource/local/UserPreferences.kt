package ch.drcookie.polaris_app.data.datasource.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import ch.drcookie.polaris_app.domain.repository.LocalPreferences

class UserPreferences(context: Context): LocalPreferences {
    private val prefs: SharedPreferences = context.getSharedPreferences("polaris_prefs", Context.MODE_PRIVATE)

    override var apiKey: String?
        get() = prefs.getString("API_KEY", null)
        set(value) = prefs.edit { putString("API_KEY", value) }

    override var phoneId: Long
        get() = prefs.getLong("PHONE_ID", -1L)
        set(value) = prefs.edit { putLong("PHONE_ID", value) }
}