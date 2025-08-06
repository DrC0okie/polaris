package ch.heig.iict.polaris_health.ui.visit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.NavigationUI
import ch.heig.iict.polaris_health.databinding.FragmentVisitDetailBinding
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

        setupToolbar()
        setupClickListeners()
        observeUiState()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}