package ch.drcookie.polaris_app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ch.drcookie.polaris_app.databinding.ActivityMainBinding
import ch.drcookie.polaris_app.ui.viewmodel.PolarisViewModel
import ch.drcookie.polaris_app.ui.viewmodel.PolarisViewModelFactory
import ch.drcookie.polaris_app.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalUnsignedTypes::class)
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: PolarisViewModel by viewModels { PolarisViewModelFactory() }

    // Lambda to hold an action that should be run after permissions are granted.
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

     // For requesting multiple permissions. It executes the `onPermissionsGranted` action on success.
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

        binding.registerButton.setOnClickListener {
            requestPermissionsAndRun {
                viewModel.fetchBeacons()
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

        binding.monitorBoradcastButton.setOnClickListener {
            requestPermissionsAndRun {
                viewModel.runEndToEndStatusCheckFlow()
            }
        }

        binding.fetchBeaconButton.setOnClickListener {
            requestPermissionsAndRun {
                viewModel.pullDataFromBeacon()
            }
        }

        // Observe UI state from the ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    /**
     * Updates the UI elements based on the current [UiState].
     */
    private fun updateUi(state: UiState) {
        binding.debugLog.text = state.log

        // Determine if any major action is in progress
        val isActionInProgress = state.isBusy || state.isMonitoring

        // Enable/disable buttons based on whether an action is in progress
        binding.TokenFlowButton.isEnabled = !isActionInProgress
        binding.payloadFlowButton.isEnabled = !isActionInProgress
        binding.registerButton.isEnabled = !isActionInProgress
        binding.fetchBeaconButton.isEnabled = !isActionInProgress

        // The monitor button should be disabled only when a *different* flow is busy
        binding.monitorBoradcastButton.isEnabled = !state.isBusy

        if (state.isMonitoring) {
            binding.monitorBoradcastButton.text = "Stop Monitoring"
        } else {
            binding.monitorBoradcastButton.text = "Start Monitoring"
        }

        // Scroll log
        binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun requestPermissionsAndRun(action: () -> Unit) {
        if (hasPermissions()) {
            // Permissions are already granted, run the action immediately.
            action()
        } else {
            // Permissions are not granted. Store the action
            onPermissionsGranted = action
            // and then request permissions. The result will be handled by the launcher.
            permissionLauncher.launch(permissions)
        }
    }

    private fun hasPermissions(): Boolean {
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}