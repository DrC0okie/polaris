package ch.heig.iict.polaris_health.ui.init

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import ch.heig.iict.polaris_health.R
import ch.heig.iict.polaris_health.databinding.FragmentLauncherBinding
import ch.heig.iict.polaris_health.di.AppContainer
import kotlinx.coroutines.launch
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan

class LauncherFragment : Fragment(R.layout.fragment_launcher) {

    private lateinit var binding: FragmentLauncherBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentLauncherBinding.bind(view)

        val full = "Polaris HEALTH"
        val span = SpannableString(full).apply {
            val start = full.indexOf("HEALTH")
            if (start >= 0) {
                setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.md_theme_primary) // ou R.color.secondaryContainer
                    ),
                    start, start + "HEALTH".length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        binding.appName.text = span

        val motion = binding.motionRoot
        motion.post { motion.transitionToEnd() }

        // Block back press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Ignore
            }
        })

        binding.lottieAnimationView.apply {
            playAnimation()
            addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (hasPermissions(requireContext())) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            AppContainer.init(requireContext().applicationContext)
                            findNavController().navigate(
                                R.id.action_launcherFragment_to_tourFragment,
                                null,
                                NavOptions.Builder()
                                    .setPopUpTo(R.id.launcherFragment, inclusive = true)
                                    .build()
                            )
                        }
                    } else {
                        findNavController().navigate(
                            R.id.action_launcherFragment_to_permissionFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.launcherFragment, inclusive = true)
                                .build()
                        )
                    }
                }
            })
        }
    }

    private fun hasPermissions(context: Context): Boolean {
        val permissions = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
