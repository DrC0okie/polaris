package ch.heig.iict.polaris_health.ui.tour

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeToRefresh()
        observeUiState()
        observeErrorEvents()
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.toggleProximityMode()
        }
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
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.swipeRefreshLayout.isRefreshing = state.isRefreshing

                    tourAdapter.submitList(state.visits)
                    binding.emptyView.isVisible = !state.isLoading && state.visits.isEmpty()

                    // On peut aussi changer le titre pour indiquer le mode
                    (requireActivity() as AppCompatActivity).supportActionBar?.subtitle =
                        if (state.isInProximityMode) "Mode de proximité actif" else ""
                }
            }
        }
    }

    private fun observeErrorEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorEvent.collect { event ->
                event.getContentIfNotHandled()?.let { errorMessage ->
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewVisits.adapter = null
        _binding = null
    }
}