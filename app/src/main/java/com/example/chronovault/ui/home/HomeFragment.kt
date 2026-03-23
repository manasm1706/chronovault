package com.example.chronovault.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chronovault.R
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.databinding.FragmentHomeBinding
import com.example.chronovault.ui.capsules.CreateCapsuleActivity
import com.example.chronovault.ui.capsules.FilterType
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.ui.map.MapFragment
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

/**
 * HomeFragment - Dashboard with capsule statistics
 * Displays daily quote, user greeting, and capsule summary cards
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var recentCapsulesAdapter: RecentCapsulesAdapter
    // FIX: 9
    private val mainHandler = Handler(Looper.getMainLooper())
    private var nearbyCountdownRunnable: Runnable? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) requestLastKnownLocation()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecentMemoriesList()
        setupUI()
        observeViewModel()
        requestLocationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDashboardData()
        requestLastKnownLocation()
    }

    private fun setupRecentMemoriesList() {
        recentCapsulesAdapter = RecentCapsulesAdapter { capsule ->
            if (!isAdded || capsule.id.isBlank()) return@RecentCapsulesAdapter
            // FIX: 14
            handleMemoryClick(capsule)
        }

        binding.rvRecentCapsules.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentCapsulesAdapter
        }
    }

    private fun setupUI() {
        binding.btnCreateCapsule.setOnClickListener {
            startActivity(Intent(requireContext(), CreateCapsuleActivity::class.java))
        }

        binding.btnRefreshQuote.setOnClickListener {
            viewModel.refreshQuote()
        }

        binding.cardTotal.setOnClickListener { navigateToCapsules(FilterType.ALL) }
        binding.cardLocked.setOnClickListener { navigateToCapsules(FilterType.LOCKED) }
        binding.cardUnlocked.setOnClickListener { navigateToCapsules(FilterType.UNLOCKED) }
        binding.cardShared.setOnClickListener { navigateToCapsules(FilterType.SHARED) }

        binding.btnViewNearbyMemory.setOnClickListener {
            // FIX: 1
            val nearby = viewModel.nearbyCapsule.value ?: return@setOnClickListener
            if (!isAdded || nearby.id.isBlank()) return@setOnClickListener

            // FIX: 1
            requireActivity().runOnUiThread {
                if (!isAdded) return@runOnUiThread
                // FIX: 14
                if (shouldOpenNearbyDetails(nearby)) {
                    openCapsuleInCenterTab(nearby.id)
                } else {
                    navigateToMapSafely(nearby.id)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvGreeting.text = getString(R.string.home_dynamic_greeting, viewModel.getGreeting(), name)
        }

        viewModel.greetingSubtitle.observe(viewLifecycleOwner) { subtitle ->
            binding.tvGreetingSubtitle.text = subtitle
        }

        viewModel.dailyQuote.observe(viewLifecycleOwner) { quote ->
            binding.tvQuote.text = getString(R.string.home_quote_wrapped, quote)
        }

        viewModel.totalCapsules.observe(viewLifecycleOwner) { count ->
            binding.tvCountTotal.text = count.toString()
        }

        viewModel.lockedCapsules.observe(viewLifecycleOwner) { count ->
            binding.tvCountLocked.text = count.toString()
        }

        viewModel.unlockedCapsules.observe(viewLifecycleOwner) { count ->
            binding.tvCountUnlocked.text = count.toString()
        }

        viewModel.sharedCapsules.observe(viewLifecycleOwner) { count ->
            binding.tvCountShared.text = count.toString()
        }

        viewModel.capsuleList.observe(viewLifecycleOwner) { capsules ->
            val hasCapsules = capsules.isNotEmpty()
            binding.layoutEmptyState.visibility = if (hasCapsules) View.GONE else View.VISIBLE
            binding.layoutStatsSection.visibility = if (hasCapsules) View.VISIBLE else View.GONE
            binding.layoutRecentSection.visibility = if (hasCapsules) View.VISIBLE else View.GONE
        }

        viewModel.recentCapsules.observe(viewLifecycleOwner) { recentCapsules ->
            recentCapsulesAdapter.submitList(recentCapsules)
        }

        viewModel.nearbyCapsule.observe(viewLifecycleOwner) { nearbyCapsule ->
            if (nearbyCapsule == null) {
                binding.cardNearbyMemory.visibility = View.GONE
                // FIX: 9
                stopNearbyCountdown()
            } else {
                binding.cardNearbyMemory.visibility = View.VISIBLE
                // FIX: 9
                updateNearbyBody(nearbyCapsule)
                // FIX: 2
                binding.btnViewNearbyMemory.text = if (shouldOpenNearbyDetails(nearbyCapsule)) {
                    getString(R.string.home_view_memory)
                } else {
                    "Go to Location"
                }
            }
        }

        viewModel.loadingState.observe(viewLifecycleOwner) { state ->
            when (state) {
                LoadingState.Loading -> binding.progressHome.visibility = View.VISIBLE
                LoadingState.Success -> binding.progressHome.visibility = View.GONE
                is LoadingState.Error -> {
                    binding.progressHome.visibility = View.GONE
                    showError(state.message)
                }
                LoadingState.Idle -> Unit
            }
        }
    }

    private fun navigateToCapsules(filterType: FilterType) {
        parentFragmentManager.setFragmentResult(
            CAPSULE_FILTER_REQUEST,
            bundleOf(KEY_FILTER_TYPE to filterType.name)
        )
        requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId = R.id.navigation_capsules
    }

    private fun requestLocationPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            requestLastKnownLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        LocationServices.getFusedLocationProviderClient(requireActivity())
            .lastLocation
            .addOnSuccessListener { location ->
                location?.let { viewModel.updateUserLocation(it.latitude, it.longitude) }
            }
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    // FIX: 14
    private fun handleMemoryClick(capsule: CapsuleEntity) {
        if (shouldOpenNearbyDetails(capsule)) {
            openCapsuleInCenterTab(capsule.id)
            return
        }
        showLockedMessage(capsule)
    }

    // FIX: 14
    private fun openCapsuleInCenterTab(capsuleId: String) {
        if (!isAdded || capsuleId.isBlank()) return
        parentFragmentManager.setFragmentResult(
            CAPSULE_OPEN_REQUEST,
            bundleOf(KEY_CAPSULE_ID to capsuleId)
        )
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.nav_view)
            .selectedItemId = R.id.navigation_capsules
    }

    // FIX: 14
    private fun showLockedMessage(capsule: CapsuleEntity) {
        val msg = when {
            capsule.isTimeBased && (capsule.unlockTime ?: 0L) > System.currentTimeMillis() -> {
                val formatted = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(capsule.unlockTime!!))
                "This memory unlocks on $formatted"
            }
            capsule.isLocationBased -> "Visit the location to unlock this memory"
            else -> "This memory is locked"
        }
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_LONG).show()
    }

    // FIX: 2
    private fun shouldOpenNearbyDetails(capsule: CapsuleEntity): Boolean {
        if (isEffectivelyUnlocked(capsule)) return true
        val unlockTime = capsule.unlockTime ?: 0L
        return unlockTime <= 0L && !capsule.isLocationBased
    }

    // FIX: 12
    private fun isEffectivelyUnlocked(capsule: CapsuleEntity): Boolean {
        if (capsule.isUnlocked) return true
        val unlockTime = capsule.unlockTime ?: 0L
        return capsule.isTimeBased && unlockTime in 1..System.currentTimeMillis()
    }

    // FIX: 9
    private fun updateNearbyBody(capsule: CapsuleEntity) {
        val unlockTime = capsule.unlockTime ?: 0L
        val isTimeLocked = capsule.isTimeBased && unlockTime > System.currentTimeMillis()

        if (!isTimeLocked) {
            stopNearbyCountdown()
            binding.tvNearbyCapsuleInfo.text = getString(R.string.home_nearby_body, capsule.title)
            return
        }

        startNearbyCountdown(capsule)
    }

    // FIX: 9
    private fun startNearbyCountdown(capsule: CapsuleEntity) {
        stopNearbyCountdown()
        nearbyCountdownRunnable = object : Runnable {
            override fun run() {
                val unlockTime = capsule.unlockTime ?: 0L
                val remainingMs = unlockTime - System.currentTimeMillis()
                if (remainingMs <= 0L || !isAdded) {
                    binding.tvNearbyCapsuleInfo.text = getString(R.string.home_nearby_body, capsule.title)
                    return
                }

                val totalSeconds = remainingMs / 1000
                val days = totalSeconds / 86_400
                val hours = (totalSeconds % 86_400) / 3_600
                val minutes = (totalSeconds % 3_600) / 60
                val seconds = totalSeconds % 60
                binding.tvNearbyCapsuleInfo.text = String.format(
                    Locale.getDefault(),
                    "%s\nUnlocks in %dd %dh %dm %ds",
                    getString(R.string.home_nearby_body, capsule.title),
                    days,
                    hours,
                    minutes,
                    seconds
                )
                mainHandler.postDelayed(this, 1000L)
            }
        }
        mainHandler.post(nearbyCountdownRunnable!!)
    }

    // FIX: 9
    private fun stopNearbyCountdown() {
        nearbyCountdownRunnable?.let { mainHandler.removeCallbacks(it) }
        nearbyCountdownRunnable = null
    }

    // FIX: 1
    private fun navigateToMapSafely(capsuleId: String) {
        if (!isAdded || capsuleId.isBlank()) return
        try {
            val navController = findNavController()
            if (navController.currentDestination?.id != R.id.navigation_home) return
            navController.navigate(
                R.id.navigation_map,
                bundleOf(MapFragment.ARG_FOCUS_CAPSULE_ID to capsuleId)
            )
        } catch (ise: IllegalStateException) {
            Log.e("HomeFragment", "Map navigation skipped: fragment not attached", ise)
        } catch (t: Throwable) {
            Log.e("HomeFragment", "Map navigation failed", t)
        }
    }

    override fun onDestroyView() {
        // FIX: 9
        stopNearbyCountdown()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val CAPSULE_FILTER_REQUEST = "home_capsule_filter_request"
        const val KEY_FILTER_TYPE = "key_filter_type"
        // FIX: 14
        const val CAPSULE_OPEN_REQUEST = "home_capsule_open_request"
        const val KEY_CAPSULE_ID = "key_capsule_id"
    }
}
