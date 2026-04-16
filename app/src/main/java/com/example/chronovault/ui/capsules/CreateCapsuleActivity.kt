package com.example.chronovault.ui.capsules

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.chronovault.R
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.databinding.ActivityCreateCapsuleBinding
import com.example.chronovault.utils.GooglePlayServicesGuard
import com.example.chronovault.utils.ThemeManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch

/**
 * CreateCapsuleActivity - Create a new time capsule
 * Handles title, message, image, location, and unlock conditions.
 * Automatically captures GPS location for the capsule.
 */
class CreateCapsuleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCapsuleBinding
    private val viewModel: CapsulesViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCaptured = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setImageFromUri(this, it)
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            captureCurrentLocation()
        } else {
            handleLocationPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val appearancePrefs = ServiceLocator.providePreferencesManager(this)
        ThemeManager.applyTheme(
            activity = this,
            modeValue = appearancePrefs.getSelectedThemeMode(),
            schemeValue = appearancePrefs.getSelectedColorScheme()
        )
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCapsuleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        observeViewModel()
        requestLocationAndCapture()
    }

    private fun requestLocationAndCapture() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                captureCurrentLocation()
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
        AlertDialog.Builder(this)
            .setTitle(R.string.location_permission_title)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(R.string.confirm) { _, _ ->
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setNegativeButton(R.string.dismiss, null)
            .show()
    }

    private fun showSettingsDialogForLocationPermission() {
        AlertDialog.Builder(this)
            .setTitle(R.string.location_permission_title)
            .setMessage(R.string.location_permission_settings_message)
            .setPositiveButton(R.string.button_open_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            }
            .setNegativeButton(R.string.dismiss, null)
            .show()
    }

    private fun handleLocationPermissionDenied() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, R.string.location_permission_rationale, Toast.LENGTH_LONG).show()
            return
        }
        showSettingsDialogForLocationPermission()
    }

    @Suppress("MissingPermission")
    private fun captureCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        if (!GooglePlayServicesGuard.warnIfUnavailable(this, "CreateCapsuleActivity")) {
            binding.btnPickLocation.text = "⚠️ Could not get location"
            return
        }

        // Try to get a fresh location first, fall back to last known
        val cancellationToken = CancellationTokenSource()
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("MAP", "Fresh location -> Lat: ${location.latitude}, Lng: ${location.longitude}")
                    viewModel.setLocation(location.latitude, location.longitude)
                    locationCaptured = true
                    binding.btnPickLocation.text = "📍 ${String.format("%.4f, %.4f", location.latitude, location.longitude)}"
                } else {
                    // Fall back to lastLocation
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            Log.d("MAP", "Last known location -> Lat: ${lastLoc.latitude}, Lng: ${lastLoc.longitude}")
                            viewModel.setLocation(lastLoc.latitude, lastLoc.longitude)
                            locationCaptured = true
                            binding.btnPickLocation.text = "📍 ${String.format("%.4f, %.4f", lastLoc.latitude, lastLoc.longitude)}"
                        } else {
                            Log.w("MAP", "Location capture failed: both current and last location are null")
                            binding.btnPickLocation.text = "⚠️ Could not get location"
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e("MAP", "getCurrentLocation failed, falling back to lastLocation", it)
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        Log.d("MAP", "Fallback last location -> Lat: ${lastLoc.latitude}, Lng: ${lastLoc.longitude}")
                        viewModel.setLocation(lastLoc.latitude, lastLoc.longitude)
                        locationCaptured = true
                        binding.btnPickLocation.text = "📍 ${String.format("%.4f, %.4f", lastLoc.latitude, lastLoc.longitude)}"
                    } else {
                        Log.w("MAP", "Fallback lastLocation is null")
                    }
                }.addOnFailureListener { fallbackError ->
                    Log.w("MAP", "Fallback lastLocation failed", fallbackError)
                }
            }
        } catch (securityException: SecurityException) {
            Log.w("MAP", "Security exception while requesting fused location", securityException)
            binding.btnPickLocation.text = "⚠️ Could not get location"
        }
    }

    private fun setupUI() {
        binding.apply {
            // Toolbar
            toolbar.setNavigationOnClickListener { finish() }

            // Image selection
            ivCapsuleImage.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }

            // Location picker - re-capture current location
            btnPickLocation.setOnClickListener {
                requestLocationAndCapture()
                Toast.makeText(this@CreateCapsuleActivity, "Refreshing location...", Toast.LENGTH_SHORT).show()
            }

            // Date picker for time-based unlock
            btnPickDate.setOnClickListener {
                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select unlock date")
                    .build()
                datePicker.addOnPositiveButtonClickListener { selection: Long ->
                    viewModel.setUnlockDate(selection)
                    btnPickDate.text = "📅 Unlock: ${formatDate(selection)}"
                }
                datePicker.show(supportFragmentManager, "date_picker")
            }

            // Location-based unlock toggle
            switchLocationBased.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setLocationBased(isChecked)
            }

            // Shareable toggle
            switchShareable.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setShareable(isChecked)
            }

            switchPublicWorld.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setPublic(isChecked)
            }

            // Create button
            btnCreate.setOnClickListener {
                viewModel.apply {
                    setTitle(etTitle.text.toString())
                    setMessage(etMessage.text.toString())
                    // FIX: 15
                    if (hasNoUnlockMethodSelected()) {
                        showNoUnlockConfirmationDialog()
                    } else {
                        createCapsule()
                    }
                }
            }

            // Cancel button
            btnCancel.setOnClickListener {
                finish()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.createCapsuleState.observe(this@CreateCapsuleActivity) { state ->
                    when (state) {
                        is CreateCapsuleState.Loading -> {
                            binding.progressCreate.visibility = android.view.View.VISIBLE
                            binding.btnCreate.isEnabled = false
                        }
                        is CreateCapsuleState.Success -> {
                            binding.progressCreate.visibility = android.view.View.GONE
                            Toast.makeText(this@CreateCapsuleActivity, "Capsule created!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        is CreateCapsuleState.Error -> {
                            binding.progressCreate.visibility = android.view.View.GONE
                            binding.btnCreate.isEnabled = true
                            Toast.makeText(this@CreateCapsuleActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        CreateCapsuleState.Idle -> {}
                    }
                }

                viewModel.capsuleImageBase64.observe(this@CreateCapsuleActivity) { base64 ->
                    base64?.let {
                        val bitmap = com.example.chronovault.utils.ImageConverter.base64ToBitmap(it)
                        bitmap?.let { bmp ->
                            binding.ivCapsuleImage.setImageBitmap(bmp)
                        }
                    }
                }
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    // FIX: 15
    private fun hasNoUnlockMethodSelected(): Boolean {
        val hasTimeUnlock = viewModel.unlockDate.value != null && (viewModel.isTimeBased.value == true)
        val hasLocationUnlock = viewModel.isLocationBased.value == true
        return !hasTimeUnlock && !hasLocationUnlock
    }

    // FIX: 15
    private fun showNoUnlockConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.create_unlockless_title)
            .setMessage(R.string.create_unlockless_message)
            .setPositiveButton(R.string.create_unlockless_continue) { _, _ ->
                viewModel.createCapsule()
            }
            .setNegativeButton(R.string.create_unlockless_go_back, null)
            .show()
    }
}

