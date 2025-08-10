package ch.heig.iict.polaris_health.ui.init

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import ch.heig.iict.polaris_health.R
import ch.heig.iict.polaris_health.di.AppContainer
import kotlinx.coroutines.launch

class PermissionFragment : Fragment(R.layout.fragment_permission) {

    private val permissions = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        // BLE scan still requires location
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            initAndNavigate()
        } else {
            Toast.makeText(
                requireContext(),
                "Permissions are required to proceed.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (hasPermissions()) {
            initAndNavigate()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun hasPermissions(): Boolean {
        return permissions.all {
            ActivityCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun initAndNavigate() {
        viewLifecycleOwner.lifecycleScope.launch {
            AppContainer.init(requireContext().applicationContext)
            findNavController().navigate(R.id.action_permissionFragment_to_tourFragment)
        }
    }
}