package ch.drcookie.polaris_app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import ch.drcookie.polaris_app.databinding.ActivityMainBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import ch.drcookie.polaris_app.ui.PolarisViewModel
import ch.drcookie.polaris_app.ui.PolarisViewModelFactory
import ch.drcookie.polaris_app.ui.UiState
import ch.drcookie.polaris_app.util.Crypto

@OptIn(ExperimentalUnsignedTypes::class)
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: PolarisViewModel by viewModels { PolarisViewModelFactory(this.application) }
    private var onPermissionsGranted: (() -> Unit)? = null

    private val permissions = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }.toTypedArray()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) {
                onPermissionsGranted?.invoke()
                onPermissionsGranted = null
            } else {
                Toast.makeText(this, "Permissions are required to use this feature.", Toast.LENGTH_LONG).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Crypto once
        lifecycleScope.launch {
            Crypto.initialize()
            if (Crypto.isInitialized) {
                Log.i("MainActivity", "Crypto Initialized Successfully.")
            }
        }

        binding.registerButton.setOnClickListener {
            requestPermissionsAndRun {
                viewModel.register()
            }
        }

        binding.TokenFlowButton.setOnClickListener {
            requestPermissionsAndRun {
                viewModel.findAndExecuteTokenFlow()
            }
        }

        binding.payloadFlowButton.setOnClickListener {
            requestPermissionsAndRun {
                viewModel.processPayloadFlow()
            }
        }

        binding.fetchBeaconsButton.isEnabled = false

        // --- Observe UI state from ViewModel ---
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: UiState) {
        binding.debugLog.text = state.log
        binding.messageBox.text = "Status: ${state.connectionStatus}"
        binding.TokenFlowButton.isEnabled = state.canStart
        binding.registerButton.isEnabled = state.canStart

        // Scroll to the bottom of the log
        binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun requestPermissionsAndRun(action: () -> Unit) {
        if (hasPermissions()) {
            // Permissions are already granted, run the action immediately.
            action()
        } else {
            // Permissions are not granted. Store the action...
            onPermissionsGranted = action
            // ...and then request permissions. The result will be handled by the launcher.
            permissionLauncher.launch(permissions)
        }
    }

    private fun hasPermissions(): Boolean {
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
