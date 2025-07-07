package ch.drcookie.polaris_app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ch.drcookie.polaris_app.databinding.ActivityMainBinding
import ch.drcookie.polaris_app.ui.viewmodel.PolarisViewModel
import ch.drcookie.polaris_app.ui.viewmodel.PolarisViewModelFactory
import ch.drcookie.polaris_app.ui.viewmodel.UiState
import ch.drcookie.polaris_sdk.api.Polaris
import kotlinx.coroutines.launch

@OptIn(ExperimentalUnsignedTypes::class)
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: PolarisViewModel by viewModels {
        PolarisViewModelFactory(applicationContext)
    }

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
                Toast.makeText(
                    this,
                    "Permissions are required to use this feature.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissionsAndRun {
            setupApplicationLogic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            Polaris.shutdown()
        }
    }

    private fun setupApplicationLogic() {
        viewModel.also {
            setupClickListeners()
            observeUiState()
        }
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            viewModel.fetchBeacons()
        }

        binding.TokenFlowButton.setOnClickListener {
            viewModel.findAndExecuteTokenFlow()
        }

        binding.payloadFlowButton.setOnClickListener {
            viewModel.processPayloadFlow()
        }

        binding.monitorBoradcastButton.setOnClickListener {
            viewModel.toggleBroadcastMonitoring()
        }

        binding.endToEndButton.setOnClickListener {
            viewModel.runEndToEndStatusCheckFlow()
        }

        binding.fetchBeaconButton.setOnClickListener {
            viewModel.pullDataFromBeacon()
        }
    }

    private fun observeUiState() {
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

        // The monitor button should be disabled only when a different flow is busy
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

            val shouldShowRationale = permissions.any {
                ActivityCompat.shouldShowRequestPermissionRationale(this, it)
            }

            if (shouldShowRationale) {
                Toast.makeText(
                    this,
                    "Bluetooth and Location permissions are essential for this app to discover beacons.",
                    Toast.LENGTH_LONG
                ).show()

                permissionLauncher.launch(permissions)
            } else {
                permissionLauncher.launch(permissions)
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}