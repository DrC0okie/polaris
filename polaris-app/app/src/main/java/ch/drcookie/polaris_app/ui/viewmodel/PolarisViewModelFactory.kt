package ch.drcookie.polaris_app.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PolarisViewModelFactory() : ViewModelProvider.Factory {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PolarisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PolarisViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}