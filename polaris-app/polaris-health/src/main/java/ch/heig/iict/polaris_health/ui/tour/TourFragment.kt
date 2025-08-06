package ch.heig.iict.polaris_health.ui.tour

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import ch.heig.iict.polaris_health.databinding.FragmentTourBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TourFragment : Fragment() {

    private var _binding: FragmentTourBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TourViewModel by viewModels { TourViewModel.Factory }

    private lateinit var tourAdapter: TourAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTourBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeUiState()
    }

    private fun setupRecyclerView() {
        tourAdapter = TourAdapter { visit ->
            if (!visit.isLocked) {
                val action = TourFragmentDirections.actionTourFragmentToVisitDetailFragment(visit.visitId)
                findNavController().navigate(action)
            }
        }
        binding.recyclerViewVisits.adapter = tourAdapter
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.progressBar.isVisible = state.isLoading
                tourAdapter.submitList(state.visits)
                binding.emptyView.isVisible = !state.isLoading && state.visits.isEmpty()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}