package ch.heig.iict.polaris_health.ui.visit

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import ch.heig.iict.polaris_health.R
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.NavigationUI
import ch.heig.iict.polaris_health.databinding.FragmentVisitDetailBinding
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.transition.platform.MaterialArcMotion
import com.google.android.material.transition.platform.MaterialContainerTransform
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VisitDetailFragment : Fragment() {

    private var _binding: FragmentVisitDetailBinding? = null
    private val binding get() = _binding!!
    private val args: VisitDetailFragmentArgs by navArgs()
    private val viewModel: VisitDetailViewModel by viewModels {
        VisitDetailViewModel.provideFactory(args.visitId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVisitDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        animate(view)
        setupToolbar()
        setupClickListeners()
        observeUiState()
        observeNavigationEvents()
    }

    private fun animate(view: View){
        val end = view.findViewById<View>(R.id.detail_container)
        end.transitionName = "visit_container_${args.visitId}"

        // Option: éviter tout “flash” si la vue met un frame à se mesurer
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        // Start/End shape pour des coins qui passent de 12dp (item) à 0dp (plein écran)
        val startShape = ShapeAppearanceModel.Builder()
            .setAllCornerSizes(resources.getDimension(R.dimen.card_corner_12dp)) // 12dp
            .build()
        val endShape = ShapeAppearanceModel.Builder()
            .setAllCornerSizes(0f)
            .build()

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment        // id racine de l’Activity
            duration = 1000
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(
                MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface)
            )
            startShapeAppearanceModel = startShape
            endShapeAppearanceModel = endShape
            fitMode = MaterialContainerTransform.FIT_MODE_AUTO
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            pathMotion = MaterialArcMotion()  // trajectoire légèrement incurvée (plus organique)
        }

        sharedElementReturnTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = 1000
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(
                MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface)
            )
            startShapeAppearanceModel = endShape
            endShapeAppearanceModel = startShape
            fitMode = MaterialContainerTransform.FIT_MODE_AUTO
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            pathMotion = MaterialArcMotion()
        }
    }

    private fun setupToolbar() {
        NavigationUI.setupWithNavController(binding.toolbarDetail, findNavController())
    }

    private fun setupClickListeners() {
        binding.buttonGenerateToken.setOnClickListener { viewModel.onGenerateTokenClicked() }
        binding.buttonCheckMaintenance.setOnClickListener { viewModel.onCheckMaintenanceClicked() }
        binding.buttonSync.setOnClickListener { viewModel.onSyncClicked() }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.progressBarDetail.isVisible = state.isBusy

                binding.patientNameDetail.text = state.patientFullName
                binding.patientDetailsText.text = state.patientDetails
                binding.logView.text = state.log
                binding.buttonGenerateToken.isEnabled = state.isTokenButtonEnabled && !state.isBusy
                binding.buttonCheckMaintenance.isEnabled = !state.isBusy
                binding.buttonSync.isEnabled = !state.isBusy
                binding.logScrollView.post { binding.logScrollView.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    private fun observeNavigationEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { event ->
                    when (event) {
                        is VisitDetailViewModel.NavigationEvent.NavigateBack -> {
                            Toast.makeText(requireContext(), "Proximité perdue, fermeture du dossier.", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}