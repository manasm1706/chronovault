package com.example.chronovault.ui.capsules

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chronovault.R
import com.example.chronovault.databinding.FragmentCapsulesBinding
import com.example.chronovault.ui.home.HomeFragment
import com.example.chronovault.ui.common.LoadingState
import kotlinx.coroutines.launch

/**
 * CapsulesFragment - Display user's capsules with filtering
 * Supports filtering by: All, Locked, Unlocked, Shared
 */
class CapsulesFragment : Fragment() {

    private var _binding: FragmentCapsulesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CapsulesViewModel by viewModels()
    private lateinit var adapter: CapsulesAdapter
    private var pendingFilter: FilterType? = null
    private var pendingOpenCapsuleId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentFragmentManager.setFragmentResultListener(
            HomeFragment.CAPSULE_FILTER_REQUEST,
            this
        ) { _, bundle ->
            val requestedFilter = bundle.getString(HomeFragment.KEY_FILTER_TYPE)
                ?.let { runCatching { FilterType.valueOf(it) }.getOrNull() }
            pendingFilter = requestedFilter
            requestedFilter?.let { applyFilter(it) }
        }

        // FIX: 14
        parentFragmentManager.setFragmentResultListener(
            HomeFragment.CAPSULE_OPEN_REQUEST,
            this
        ) { _, bundle ->
            val capsuleId = bundle.getString(HomeFragment.KEY_CAPSULE_ID)?.takeIf { it.isNotBlank() }
            pendingOpenCapsuleId = capsuleId
            capsuleId?.let { navigateToDetails(it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCapsulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        // Filter buttons
        binding.chipAll.setOnClickListener { applyFilter(FilterType.ALL) }
        binding.chipLocked.setOnClickListener { applyFilter(FilterType.LOCKED) }
        binding.chipUnlocked.setOnClickListener { applyFilter(FilterType.UNLOCKED) }
        binding.chipShared.setOnClickListener { applyFilter(FilterType.SHARED) }

        // FAB for create capsule
        binding.fabCreateCapsule.setOnClickListener {
            startActivity(Intent(requireContext(), CreateCapsuleActivity::class.java))
        }

        pendingFilter?.let {
            applyFilter(it)
            pendingFilter = null
        } ?: updateFilterUi(FilterType.ALL)

        // FIX: 14
        pendingOpenCapsuleId?.let {
            navigateToDetails(it)
            pendingOpenCapsuleId = null
        }
    }

    private fun applyFilter(filter: FilterType) {
        updateFilterUi(filter)
        viewModel.setFilter(filter)
    }

    private fun updateFilterUi(filter: FilterType) {
        val selectedColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), com.example.chronovault.R.color.primary))
        val unselectedColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), com.example.chronovault.R.color.surface))

        val chips = mapOf(
            binding.chipAll to FilterType.ALL,
            binding.chipLocked to FilterType.LOCKED,
            binding.chipUnlocked to FilterType.UNLOCKED,
            binding.chipShared to FilterType.SHARED
        )

        chips.forEach { (chip, chipFilter) ->
            val isSelected = chipFilter == filter
            chip.chipBackgroundColor = if (isSelected) selectedColor else unselectedColor
            chip.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isSelected) com.example.chronovault.R.color.white else com.example.chronovault.R.color.text_primary
                )
            )
        }
    }

    private fun setupRecyclerView() {
        adapter = CapsulesAdapter { capsule -> onCapsuleClick(capsule) }

        binding.rvCapsules.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@CapsulesFragment.adapter
        }
    }

    private fun onCapsuleClick(capsule: com.example.chronovault.data.local.entity.CapsuleEntity) {
        Log.d("CapsuleClick", "Clicked: ${capsule.id}, unlocked=${capsule.isUnlocked}")

        if (viewModel.canOpenCapsule(capsule)) {
            navigateToDetails(capsule.id)
        } else {
            showLockedMessage(capsule)
        }
    }

    private fun navigateToDetails(id: String) {
        if (!isAdded || id.isBlank()) return

        // FIX: 14
        runCatching {
            val navController = findNavController()
            if (navController.currentDestination?.id != R.id.navigation_capsules) return
            navController.navigate(
                R.id.action_capsulesFragment_to_capsuleDetailsActivity,
                bundleOf(CAPSULE_ID_ARG to id)
            )
            pendingOpenCapsuleId = null
        }.onFailure { throwable ->
            Log.e("CapsuleClick", "Navigation failed for capsuleId=$id", throwable)
            pendingOpenCapsuleId = id
        }
    }

    override fun onResume() {
        super.onResume()
        // FIX: 14
        pendingOpenCapsuleId?.let {
            navigateToDetails(it)
        }
    }

    companion object {
        // FIX: 14
        const val CAPSULE_ID_ARG = "capsule_id"
    }

    private fun showLockedMessage(capsule: com.example.chronovault.data.local.entity.CapsuleEntity) {
        // FIX: 4
        AlertDialog.Builder(requireContext())
            .setTitle(getString(com.example.chronovault.R.string.status_locked))
            .setMessage(viewModel.getLockedMessage(capsule))
            .setPositiveButton(com.example.chronovault.R.string.dismiss, null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.capsulesList.observe(viewLifecycleOwner) { capsules ->
                    adapter.submitList(capsules)
                }

                viewModel.loadingState.observe(viewLifecycleOwner) { state ->
                    when (state) {
                        LoadingState.Loading -> binding.progressCapsules.visibility = View.VISIBLE
                        LoadingState.Success -> binding.progressCapsules.visibility = View.GONE
                        is LoadingState.Error -> {
                            binding.progressCapsules.visibility = View.GONE
                            showError(state.message)
                        }
                        LoadingState.Idle -> {}
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

