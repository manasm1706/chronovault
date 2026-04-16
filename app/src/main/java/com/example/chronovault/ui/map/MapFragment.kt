package com.example.chronovault.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.animation.ValueAnimator
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
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
import com.example.chronovault.utils.GooglePlayServicesGuard
import com.example.chronovault.utils.LocationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import android.view.animation.LinearInterpolator
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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
    private var activeClueArea: ClueArea? = null
    private var clueOverlay: Polygon? = null
    private var pendingDiscoveredCapsuleId: String? = null
    private val markerPulseAnimators = mutableListOf<ValueAnimator>()
    private var overlayOptions = MapViewModel.MapOverlayOptions()

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

        parentFragmentManager.setFragmentResultListener(MAP_FOCUS_REQUEST, viewLifecycleOwner) { _, bundle ->
            pendingFocusCapsuleId = bundle.getString(KEY_FOCUS_CAPSULE_ID)
            viewModel.allCapsules.value?.let { updateMapMarkers(it, shouldRecenter = true) }
        }

        parentFragmentManager.setFragmentResultListener(MAP_CLUE_REQUEST, viewLifecycleOwner) { _, bundle ->
            val latitude = bundle.getDouble(KEY_CLUE_LATITUDE, 0.0)
            val longitude = bundle.getDouble(KEY_CLUE_LONGITUDE, 0.0)
            val capsuleId = bundle.getString(KEY_CLUE_CAPSULE_ID).orEmpty()
            val title = bundle.getString(KEY_CLUE_TITLE).orEmpty()
            if (latitude == 0.0 && longitude == 0.0) return@setFragmentResultListener

            activeClueArea = buildClueArea(latitude, longitude, capsuleId, title)
            renderClueOverlay()
            Toast.makeText(requireContext(), R.string.map_clue_toast, Toast.LENGTH_SHORT).show()
        }

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", 0)
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName

        if (GooglePlayServicesGuard.warnIfUnavailable(requireContext(), "MapFragment")) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        }

        setupMap()
        // FIX: 15
        setupModeToggle()
        setupMapControls()
        observeViewModel()
        requestLocationPermissions()
    }

    private fun setupMapControls() {
        binding.btnCenterMap.setOnClickListener {
            val location = viewModel.userLocation.value
            if (location != null) {
                centerOnPoint(GeoPoint(location.first, location.second))
            } else {
                requestLocationPermissions()
                Toast.makeText(requireContext(), R.string.map_center_location_unavailable, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnMapOptions.setOnClickListener { anchor ->
            showMapOptionsMenu(anchor)
        }
    }

    private fun showMapOptionsMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.map_overlay_menu, popup.menu)

        popup.menu.findItem(R.id.action_toggle_clue_circles)?.isChecked = overlayOptions.showClueCircles
        popup.menu.findItem(R.id.action_toggle_nearby_waves)?.isChecked = overlayOptions.showNearbyWaves
        popup.menu.findItem(R.id.action_toggle_my_location)?.isChecked = overlayOptions.showMyLocation
        popup.menu.findItem(R.id.action_toggle_discovery_overlay)?.isChecked = overlayOptions.showDiscoveryOverlay

        popup.setOnMenuItemClickListener { item ->
            handleMapOptionSelection(item)
            true
        }
        popup.show()
    }

    private fun handleMapOptionSelection(item: MenuItem) {
        val checked = !item.isChecked
        item.isChecked = checked
        when (item.itemId) {
            R.id.action_toggle_clue_circles -> viewModel.setShowClueCircles(checked)
            R.id.action_toggle_nearby_waves -> viewModel.setShowNearbyWaves(checked)
            R.id.action_toggle_my_location -> viewModel.setShowMyLocation(checked)
            R.id.action_toggle_discovery_overlay -> viewModel.setShowDiscoveryOverlay(checked)
        }
    }

    // FIX: 15
    private fun setupModeToggle() {
        binding.toggleMapMode.check(R.id.btn_mode_personal)

        binding.btnModePersonal.setOnClickListener {
            viewModel.setMapMode(MapViewModel.MapMode.PERSONAL)
        }

        binding.btnModeWorld.setOnClickListener {
            if (viewModel.userLocation.value == null) {
                Toast.makeText(requireContext(), R.string.location_permission_rationale, Toast.LENGTH_SHORT).show()
            }
            viewModel.setMapMode(MapViewModel.MapMode.WORLD)
        }
    }

    private fun setupMap() {
        mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
    }

    private fun observeViewModel() {
        viewModel.loadingState.observe(viewLifecycleOwner) { state ->
            binding.progressMap.visibility = when (state) {
                com.example.chronovault.ui.common.LoadingState.Loading -> View.VISIBLE
                else -> View.GONE
            }
        }

        viewModel.allCapsules.observe(viewLifecycleOwner) { capsules ->
            updateMapMarkers(capsules, shouldRecenter = true)
        }

        viewModel.discoveryEvent.observe(viewLifecycleOwner) { discoveredCapsule ->
            if (discoveredCapsule == null) return@observe
            pendingDiscoveredCapsuleId = discoveredCapsule.id
            showDiscoveryOverlay(discoveredCapsule.title)
            viewModel.consumeDiscoveryEvent()
        }

        // FIX: 15
        viewModel.mapMode.observe(viewLifecycleOwner) { mode ->
            val selectedId = if (mode == MapViewModel.MapMode.PERSONAL) {
                R.id.btn_mode_personal
            } else {
                R.id.btn_mode_world
            }
            if (binding.toggleMapMode.checkedButtonId != selectedId) {
                binding.toggleMapMode.check(selectedId)
            }
        }

        viewModel.userLocation.observe(viewLifecycleOwner) { location ->
            if (viewModel.allCapsules.value.orEmpty().isEmpty()) {
                location?.let { centerOnPoint(GeoPoint(it.first, it.second)) }
            }
        }

        viewModel.overlayOptions.observe(viewLifecycleOwner) { options ->
            overlayOptions = options
            applyOverlayOptions()
        }
    }

    private fun applyOverlayOptions() {
        if (!overlayOptions.showMyLocation) {
            myLocationOverlay?.disableMyLocation()
            myLocationOverlay?.let { mapView.overlays.remove(it) }
        } else {
            enableMyLocation()
        }

        if (!overlayOptions.showDiscoveryOverlay) {
            hideDiscoveryOverlay()
        }

        renderClueOverlay()
        updateMapMarkers(viewModel.allCapsules.value.orEmpty(), shouldRecenter = false)
    }

    private fun updateMapMarkers(capsules: List<CapsuleEntity>, shouldRecenter: Boolean = true) {
        clearMarkerPulseAnimations()
        // FIX: 6
        mapView.overlays.clear()
        if (overlayOptions.showMyLocation) {
            myLocationOverlay?.let { mapView.overlays.add(it) }
        }

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
                animateMarkerBounce(marker)
                // FIX: 10
                handleMarkerSelection(sortedCapsules)
                true
            }
            mapView.overlays.add(marker)

            if (overlayOptions.showNearbyWaves && isNearbyCapsule(primaryCapsule)) {
                startNearbyPulse(marker)
            }
        }

        // FIX: 2
        val focusCapsule = pendingFocusCapsuleId?.let { focusId ->
            validCapsules.firstOrNull { it.id == focusId }
        }

        if (shouldRecenter) {
            if (focusCapsule != null) {
                centerOnPoint(GeoPoint(focusCapsule.latitude, focusCapsule.longitude))
                pendingFocusCapsuleId = null
            } else if (activeClueArea != null) {
                centerOnPoint(activeClueArea!!.center)
            } else {
                centerOnPoint(GeoPoint(validCapsules.first().latitude, validCapsules.first().longitude))
            }
        }

        renderClueOverlay()
        mapView.invalidate()
    }

    private fun renderClueOverlay() {
        clueOverlay?.let { mapView.overlays.remove(it) }
        val clue = activeClueArea ?: return
        if (!overlayOptions.showClueCircles) {
            clueOverlay = null
            mapView.invalidate()
            return
        }
        val circleOverlay = Polygon().apply {
            points = Polygon.pointsAsCircle(clue.center, clue.radiusMeters.toDouble())
            fillColor = ContextCompat.getColor(requireContext(), R.color.warning) and 0x40FFFFFF
            strokeColor = ContextCompat.getColor(requireContext(), R.color.warning)
            strokeWidth = 4f
            title = clue.title.ifBlank { "Clue area" }
        }
        clueOverlay = circleOverlay
        mapView.overlays.add(circleOverlay)
    }

    private fun buildClueArea(
        latitude: Double,
        longitude: Double,
        capsuleId: String,
        title: String
    ): ClueArea {
        val radiusMeters = 1000f
        val offsetMeters = 250.0
        val bearingDeg = ((capsuleId.hashCode().toLong() and 0x7fffffffL) % 360).toDouble()
        val center = destinationPoint(latitude, longitude, offsetMeters, bearingDeg)
        return ClueArea(center = center, radiusMeters = radiusMeters, title = title)
    }

    private fun destinationPoint(lat: Double, lon: Double, distanceMeters: Double, bearingDegrees: Double): GeoPoint {
        val earthRadius = 6_371_000.0
        val angularDistance = distanceMeters / earthRadius
        val bearing = Math.toRadians(bearingDegrees)
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)

        val newLat = asin(
            sin(latRad) * cos(angularDistance) +
                    cos(latRad) * sin(angularDistance) * cos(bearing)
        )
        val newLon = lonRad + atan2(
            sin(bearing) * sin(angularDistance) * cos(latRad),
            cos(angularDistance) - sin(latRad) * sin(newLat)
        )

        return GeoPoint(Math.toDegrees(newLat), Math.toDegrees(newLon))
    }

    // FIX: 6
    private fun getMarkerIcon(capsule: CapsuleEntity): Drawable? {
        val pin = ContextCompat.getDrawable(requireContext(), R.drawable.marker_pin)?.mutate() ?: return null
        // FIX: 10
        val isOwned = viewModel.isOwnedByCurrentUser(capsule)
        val isNearby = isNearbyCapsule(capsule)
        val tint = when {
            isNearby -> ContextCompat.getColor(requireContext(), R.color.warning)
            capsule.isPublic -> ContextCompat.getColor(requireContext(), R.color.secondary)
            capsule.isSharedWithMe || capsule.sharedWith.isNotEmpty() -> ContextCompat.getColor(requireContext(), R.color.secondary)
            isOwned -> ContextCompat.getColor(requireContext(), R.color.success)
            else -> ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        }

        val wrapped = DrawableCompat.wrap(pin)
        DrawableCompat.setTint(wrapped, tint)
        return wrapped
    }

    private fun isNearbyCapsule(capsule: CapsuleEntity): Boolean {
        val location = viewModel.userLocation.value ?: return false
        val distance = LocationHelper.calculateDistance(
            location.first,
            location.second,
            capsule.latitude,
            capsule.longitude
        )
        return distance <= 50f
    }

    private fun startNearbyPulse(marker: Marker) {
        val drawable = marker.icon?.mutate() ?: return
        val animator = ValueAnimator.ofInt(130, 255).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                drawable.alpha = animation.animatedValue as Int
                marker.icon = drawable
                mapView.invalidate()
            }
            start()
        }
        markerPulseAnimators.add(animator)
    }

    private fun clearMarkerPulseAnimations() {
        markerPulseAnimators.forEach { it.cancel() }
        markerPulseAnimators.clear()
    }

    private fun animateMarkerBounce(marker: Marker) {
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260L
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val bounce = kotlin.math.sin(progress * Math.PI).toFloat() * 0.08f
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM + bounce)
                mapView.invalidate()
            }
            start()
        }
        markerPulseAnimators.add(animator)
    }

    private fun showDiscoveryOverlay(capsuleTitle: String) {
        if (!overlayOptions.showDiscoveryOverlay) return
        binding.tvDiscoveryCapsule.text = capsuleTitle
        binding.layoutDiscoveryOverlay.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(220L).start()
        }
        binding.cardDiscovery.apply {
            scaleX = 0.92f
            scaleY = 0.92f
            alpha = 0f
            animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(260L).start()
        }

        binding.btnOpenDiscoveredMemory.setOnClickListener {
            val id = pendingDiscoveredCapsuleId
            if (!id.isNullOrBlank()) {
                openCapsuleDetailsById(id)
            }
            hideDiscoveryOverlay()
        }

        binding.layoutDiscoveryOverlay.setOnClickListener {
            hideDiscoveryOverlay()
        }
    }

    private fun hideDiscoveryOverlay() {
        binding.layoutDiscoveryOverlay.animate().alpha(0f).setDuration(180L).withEndAction {
            binding.layoutDiscoveryOverlay.visibility = View.GONE
        }.start()
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

    private fun openCapsuleDetailsById(capsuleId: String) {
        if (!isAdded || capsuleId.isBlank()) return
        runCatching {
            val intent = Intent(requireContext(), CapsuleDetailsActivity::class.java).apply {
                putExtra("capsule_id", capsuleId)
            }
            startActivity(intent)
        }.onFailure { throwable ->
            Log.e("MapFragment", "Failed to open discovered capsule details", throwable)
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
            if (!overlayOptions.showMyLocation) return
            if (!GooglePlayServicesGuard.warnIfUnavailable(requireContext(), "MapFragment")) return
            if (!::fusedLocationClient.isInitialized) return
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
                }.addOnFailureListener { throwable ->
                    Log.w("MapFragment", "Failed to get last location from fused provider", throwable)
                }
            }
        } catch (e: SecurityException) {
            Log.w("MapFragment", "Location security exception from fused provider", e)
        } catch (e: Exception) {
            Log.e("MapFragment", "Error enabling my location", e)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mapView.isInitialized) mapView.onResume()
        if (overlayOptions.showMyLocation) {
            enableMyLocation()
        }
        // Reload map data when returning (e.g. after creating a new capsule)
        viewModel.loadMapData()
    }

    override fun onPause() {
        super.onPause()
        myLocationOverlay?.disableMyLocation()
        if (::mapView.isInitialized) mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clearMarkerPulseAnimations()
        myLocationOverlay?.disableMyLocation()
        _binding = null
    }

    data class ClueArea(
        val center: GeoPoint,
        val radiusMeters: Float,
        val title: String
    )

    companion object {
        const val ARG_FOCUS_CAPSULE_ID = "focus_capsule_id"
        const val MAP_FOCUS_REQUEST = "map_focus_request"
        const val KEY_FOCUS_CAPSULE_ID = "key_focus_capsule_id"
        const val MAP_CLUE_REQUEST = "map_clue_request"
        const val KEY_CLUE_CAPSULE_ID = "key_clue_capsule_id"
        const val KEY_CLUE_LATITUDE = "key_clue_latitude"
        const val KEY_CLUE_LONGITUDE = "key_clue_longitude"
        const val KEY_CLUE_TITLE = "key_clue_title"
    }
}
