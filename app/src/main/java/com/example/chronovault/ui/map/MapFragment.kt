package com.example.chronovault.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.chronovault.R
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.databinding.FragmentMapBinding
import com.example.chronovault.ui.capsules.CapsuleDetailsActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * MapFragment - Display capsules on OpenStreetMap via OSMDroid
 * Shows user location and capsule markers with color-coded status
 */
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()

    private lateinit var mapView: MapView
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // FIX: 2
    private var pendingFocusCapsuleId: String? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onLocationPermissionGranted(isGranted)
        if (isGranted) {
            enableMyLocation()
        } else {
            handlePermissionDenied()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // FIX: 2
        pendingFocusCapsuleId = arguments?.getString(ARG_FOCUS_CAPSULE_ID)

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", 0)
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupMap()
        observeViewModel()
        requestLocationPermissions()
    }

    private fun setupMap() {
        mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
    }

    private fun observeViewModel() {
        viewModel.allCapsules.observe(viewLifecycleOwner) { capsules ->
            updateMapMarkers(capsules)
        }

        viewModel.userLocation.observe(viewLifecycleOwner) { location ->
            if (viewModel.allCapsules.value.orEmpty().isEmpty()) {
                location?.let { centerOnPoint(GeoPoint(it.first, it.second)) }
            }
        }
    }

    private fun updateMapMarkers(capsules: List<CapsuleEntity>) {
        // FIX: 6
        mapView.overlays.clear()
        myLocationOverlay?.let { mapView.overlays.add(it) }

        val validCapsules = capsules.filterNot { it.latitude == 0.0 && it.longitude == 0.0 }
        if (validCapsules.isEmpty()) {
            viewModel.userLocation.value?.let { centerOnPoint(GeoPoint(it.first, it.second)) }
            mapView.invalidate()
            return
        }

        // FIX: 10
        val groupedByCoordinate = validCapsules.groupBy { capsule ->
            "${"%.6f".format(java.util.Locale.US, capsule.latitude)},${"%.6f".format(java.util.Locale.US, capsule.longitude)}"
        }

        groupedByCoordinate.values.forEach { capsulesAtPoint ->
            val sortedCapsules = capsulesAtPoint.sortedWith(
                compareByDescending<CapsuleEntity> { viewModel.isOwnedByCurrentUser(it) }
                    .thenByDescending { it.createdAt }
            )

            val primaryCapsule = sortedCapsules.first()
            val geoPoint = GeoPoint(primaryCapsule.latitude, primaryCapsule.longitude)
            Log.d("MAP", "Lat: ${primaryCapsule.latitude}, Lng: ${primaryCapsule.longitude}")

            val marker = Marker(mapView)
            marker.position = geoPoint
            marker.title = if (sortedCapsules.size > 1) {
                "${sortedCapsules.size} memories here"
            } else {
                primaryCapsule.title
            }
            marker.snippet = viewModel.getCapsuleStatus(primaryCapsule)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            getMarkerIcon(primaryCapsule)?.let { marker.icon = it }

            marker.setOnMarkerClickListener { _, _ ->
                // FIX: 10
                handleMarkerSelection(sortedCapsules)
                true
            }
            mapView.overlays.add(marker)
        }

        // FIX: 2
        val focusCapsule = pendingFocusCapsuleId?.let { focusId ->
            validCapsules.firstOrNull { it.id == focusId }
        }

        if (focusCapsule != null) {
            centerOnPoint(GeoPoint(focusCapsule.latitude, focusCapsule.longitude))
            pendingFocusCapsuleId = null
        } else {
            centerOnPoint(GeoPoint(validCapsules.first().latitude, validCapsules.first().longitude))
        }
        mapView.invalidate()
    }

    // FIX: 6
    private fun getMarkerIcon(capsule: CapsuleEntity): Drawable? {
        val pin = ContextCompat.getDrawable(requireContext(), R.drawable.marker_pin)?.mutate() ?: return null
        // FIX: 10
        val isOwned = viewModel.isOwnedByCurrentUser(capsule)
        val isEffectivelyUnlocked = viewModel.isEffectivelyUnlocked(capsule)
        val tint = when {
            isOwned && isEffectivelyUnlocked -> ContextCompat.getColor(requireContext(), R.color.success)
            isOwned && capsule.isTimeBased && (capsule.unlockTime ?: 0L) > System.currentTimeMillis() -> ContextCompat.getColor(requireContext(), R.color.warning)
            isOwned && capsule.isLocationBased -> ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
            isOwned -> ContextCompat.getColor(requireContext(), R.color.error)
            capsule.isSharedWithMe || capsule.sharedWith.isNotEmpty() -> ContextCompat.getColor(requireContext(), R.color.purple_700)
            isEffectivelyUnlocked -> ContextCompat.getColor(requireContext(), R.color.success)
            capsule.isTimeBased && (capsule.unlockTime ?: 0L) > System.currentTimeMillis() -> ContextCompat.getColor(requireContext(), R.color.warning)
            capsule.isLocationBased -> ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
            else -> ContextCompat.getColor(requireContext(), R.color.error)
        }

        val wrapped = DrawableCompat.wrap(pin)
        DrawableCompat.setTint(wrapped, tint)
        return wrapped
    }

    // FIX: 10
    private fun handleMarkerSelection(capsulesAtPoint: List<CapsuleEntity>) {
        if (!isAdded || capsulesAtPoint.isEmpty()) return

        if (capsulesAtPoint.size == 1) {
            openCapsuleDetails(capsulesAtPoint.first())
            return
        }

        val titles = capsulesAtPoint.map { capsule ->
            if (viewModel.isOwnedByCurrentUser(capsule)) "${capsule.title} (You)" else capsule.title
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Choose a memory")
            .setItems(titles) { _, which ->
                capsulesAtPoint.getOrNull(which)?.let { openCapsuleDetails(it) }
            }
            .setNegativeButton(R.string.dismiss, null)
            .show()
    }

    // FIX: 10
    private fun openCapsuleDetails(capsule: CapsuleEntity) {
        if (!isAdded || capsule.id.isBlank()) return
        runCatching {
            val intent = Intent(requireContext(), CapsuleDetailsActivity::class.java).apply {
                putExtra("capsule_id", capsule.id)
            }
            startActivity(intent)
        }.onFailure { throwable ->
            Log.e("MapFragment", "Failed to open capsule details from marker", throwable)
        }
    }

    private fun requestLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.onLocationPermissionGranted(true)
                enableMyLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showLocationRationaleDialog()
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun showLocationRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.location_permission_title)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(R.string.confirm) { _, _ ->
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setNegativeButton(R.string.dismiss, null)
            .show()
    }

    private fun handlePermissionDenied() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            showLocationRationaleDialog()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.location_permission_title)
            .setMessage(R.string.location_permission_settings_message)
            .setPositiveButton(R.string.button_open_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                })
            }
            .setNegativeButton(R.string.dismiss, null)
            .show()
    }

    private fun centerOnPoint(point: GeoPoint) {
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(point)
    }

    private fun enableMyLocation() {
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Add OSMDroid location overlay
                val locationProvider = GpsMyLocationProvider(requireContext())
                myLocationOverlay = MyLocationNewOverlay(locationProvider, mapView)
                myLocationOverlay?.enableMyLocation()
                mapView.overlays.add(myLocationOverlay)

                // Get last known location via FusedLocationClient
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        viewModel.setUserLocation(it.latitude, it.longitude)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MapFragment", "Error enabling my location", e)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mapView.isInitialized) mapView.onResume()
        // Reload map data when returning (e.g. after creating a new capsule)
        viewModel.loadMapData()
    }

    override fun onPause() {
        super.onPause()
        if (::mapView.isInitialized) mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        myLocationOverlay?.disableMyLocation()
        _binding = null
    }

    companion object {
        const val ARG_FOCUS_CAPSULE_ID = "focus_capsule_id"
    }
}
