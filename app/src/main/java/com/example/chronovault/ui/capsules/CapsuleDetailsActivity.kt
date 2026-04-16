package com.example.chronovault.ui.capsules

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chronovault.R
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.databinding.ActivityCapsuleDetailsBinding
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.CountdownFormatter
import com.example.chronovault.utils.GooglePlayServicesGuard
import com.example.chronovault.utils.ImageConverter
import com.example.chronovault.utils.LocationHelper
import com.example.chronovault.utils.ThemeManager
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * CapsuleDetailsActivity - View capsule details with comments and sharing management
 */
class CapsuleDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCapsuleDetailsBinding
    private val viewModel: CapsuleDetailsViewModel by viewModels()
    private var capsuleId: String? = null

    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var sharedWithAdapter: SharedWithAdapter
    private val mainHandler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null
    private var lastKnownUserLocation: Pair<Double, Double>? = null
    private var isCurrentlyLockedByGate: Boolean = false
    private var wasUnlockedByGatePreviously: Boolean? = null
    private var unlockTransitionPlayed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val appearancePrefs = ServiceLocator.providePreferencesManager(this)
        ThemeManager.applyTheme(
            activity = this,
            modeValue = appearancePrefs.getSelectedThemeMode(),
            schemeValue = appearancePrefs.getSelectedColorScheme()
        )
        super.onCreate(savedInstanceState)
        binding = ActivityCapsuleDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        unlockTransitionPlayed = savedInstanceState?.getBoolean(KEY_UNLOCK_TRANSITION_PLAYED, false) ?: false

        // FIX: unlocked-memory-crash
        capsuleId = resolveCapsuleIdFromIntent()
        if (capsuleId.isNullOrBlank()) {
            Log.e("CapsuleDetails", "Missing capsule_id in intent extras")
            Toast.makeText(this, "Unable to open this memory", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        capsuleId?.let { viewModel.loadCapsule(it) }
        fetchLastKnownLocation()
        supportFragmentManager.setFragmentResultListener(
            ShareCapsuleBottomSheet.RESULT_KEY,
            this
        ) { _, bundle ->
            val selected = bundle.getStringArray(ShareCapsuleBottomSheet.RESULT_SELECTED_USER_IDS)?.toList().orEmpty()
            if (selected.isNotEmpty()) {
                viewModel.shareCapsuleToUsers(selected)
            }
        }

        setupAdapters()
        setupUI()
        observeViewModel()
    }

    // FIX: unlocked-memory-crash
    private fun resolveCapsuleIdFromIntent(): String? {
        val direct = intent?.getStringExtra("capsule_id")?.takeIf { it.isNotBlank() }
        if (direct != null) return direct

        val extrasValue = intent?.extras?.getString("capsule_id")?.takeIf { it.isNotBlank() }
        if (extrasValue != null) return extrasValue

        return intent
            ?.extras
            ?.getBundle("android-support-nav:controller:deepLinkExtras")
            ?.getString("capsule_id")
            ?.takeIf { it.isNotBlank() }
    }

    private fun setupAdapters() {
        commentsAdapter = CommentsAdapter(
            currentUserId = viewModel.getCurrentUserId(),
            onDeleteClick = { comment -> viewModel.deleteComment(comment) }
        )
        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(this@CapsuleDetailsActivity)
            adapter = commentsAdapter
        }

        sharedWithAdapter = SharedWithAdapter(
            onRemoveClick = { user -> viewModel.unshareCapsule(user.userId) }
        )
        binding.rvSharedWith.apply {
            layoutManager = LinearLayoutManager(this@CapsuleDetailsActivity)
            adapter = sharedWithAdapter
        }
    }

    private fun setupUI() {
        binding.apply {
            toolbar.setNavigationOnClickListener { finish() }

            btnUnlock.setOnClickListener {
                viewModel.unlockCapsule()
            }

            btnShare.setOnClickListener {
                ShareCapsuleBottomSheet().show(supportFragmentManager, "share_capsule_sheet")
            }

            btnMakePrivate.setOnClickListener {
                showMakePrivateConfirmation()
            }

            btnDelete.setOnClickListener {
                showDeleteConfirmation()
            }

            btnSendComment.setOnClickListener {
                val text = etComment.text.toString()
                if (text.isNotBlank()) {
                    viewModel.addComment(text)
                    etComment.text?.clear()
                } else {
                    Toast.makeText(this@CapsuleDetailsActivity, R.string.comment_empty_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.capsule.observe(this@CapsuleDetailsActivity) { capsule ->
            capsule?.let { bindCapsuleData(it) }
        }

        viewModel.isOwner.observe(this@CapsuleDetailsActivity) { isOwner ->
            binding.btnDelete.visibility = if (isOwner) View.VISIBLE else View.GONE
            // FIX: 3
            binding.btnShare.visibility = if (isOwner && !isCurrentlyLockedByGate) View.VISIBLE else View.GONE
            binding.layoutSharedWith.visibility = if (isOwner && !isCurrentlyLockedByGate) View.VISIBLE else View.GONE
        }

        viewModel.isSharedCapsule.observe(this@CapsuleDetailsActivity) { isShared ->
            val isOwner = viewModel.isOwner.value == true
            // FIX: 3
            binding.btnMakePrivate.visibility = if (isShared && isOwner && !isCurrentlyLockedByGate) View.VISIBLE else View.GONE
        }

        viewModel.canComment.observe(this@CapsuleDetailsActivity) { canComment ->
            val showComments = canComment
            binding.commentInputLayout.visibility = if (showComments) View.VISIBLE else View.GONE
            binding.layoutComments.visibility = if (showComments) View.VISIBLE else View.GONE
        }

        viewModel.unlockReason.observe(this@CapsuleDetailsActivity) { reason ->
            binding.tvUnlockReason.text = reason
        }

        viewModel.sharedUsers.observe(this@CapsuleDetailsActivity) { sharedUsers ->
            sharedWithAdapter.submitList(sharedUsers)
            binding.tvNoShares.visibility = if (sharedUsers.isEmpty()) View.VISIBLE else View.GONE
            binding.rvSharedWith.visibility = if (sharedUsers.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.comments.observe(this@CapsuleDetailsActivity) { commentList ->
            commentsAdapter.submitList(commentList)
            binding.tvNoComments.visibility = if (commentList.isEmpty()) View.VISIBLE else View.GONE
            binding.rvComments.visibility = if (commentList.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.actionState.observe(this@CapsuleDetailsActivity) { state ->
            handleActionState(state)
        }

        viewModel.loadingState.observe(this@CapsuleDetailsActivity) { state ->
            when (state) {
                LoadingState.Loading -> binding.progressDetails.visibility = View.VISIBLE
                LoadingState.Success -> binding.progressDetails.visibility = View.GONE
                is LoadingState.Error -> {
                    binding.progressDetails.visibility = View.GONE
                    Toast.makeText(this@CapsuleDetailsActivity, state.message, Toast.LENGTH_SHORT).show()
                }
                LoadingState.Idle -> {}
            }
        }
    }

    private fun bindCapsuleData(capsule: com.example.chronovault.data.local.entity.CapsuleEntity) {
        // FIX: 3
        applyLockGate(capsule)

        if (!isCapsuleUnlockedByGate(capsule)) {
            return
        }

        stopCountdown()
        binding.apply {
            tvTitle.text = capsule.title
            tvDate.text = getString(R.string.label_created) + ": " + formatDate(capsule.createdAt)

            // FIX: 3
            tvMessage.text = capsule.message
            tvMessage.visibility = View.VISIBLE
            btnUnlock.text = getString(R.string.status_unlocked)
            btnUnlock.isEnabled = false

            if (capsule.latitude != 0.0 || capsule.longitude != 0.0) {
                tvLocation.text = String.format(Locale.US, "📍 %.4f, %.4f", capsule.latitude, capsule.longitude)
                tvLocation.visibility = View.VISIBLE
            } else {
                tvLocation.visibility = View.GONE
            }

            // FIX: unlocked-memory-crash
            runCatching {
                val bitmap = capsule.imageBase64?.let { ImageConverter.base64ToBitmap(it) }
                if (bitmap != null) {
                    ivCapsuleImage.setImageBitmap(bitmap)
                } else {
                    ivCapsuleImage.setImageDrawable(null)
                }
            }.onFailure { throwable ->
                Log.e("CapsuleDetails", "Image decode failed for capsuleId=${capsule.id}", throwable)
                ivCapsuleImage.setImageDrawable(null)
            }
        }
    }

    // FIX: 3
    private fun applyLockGate(capsule: com.example.chronovault.data.local.entity.CapsuleEntity) {
        val unlockTime = capsule.unlockTime ?: 0L
        val now = System.currentTimeMillis()

        if (unlockTime > 0L && now < unlockTime) {
            showTimeLockedState(capsule, unlockTime)
            return
        }

        if (capsule.isLocationBased) {
            val unlockLat = capsule.unlockLatitude ?: capsule.latitude
            val unlockLon = capsule.unlockLongitude ?: capsule.longitude
            val distance = lastKnownUserLocation?.let { user ->
                LocationHelper.calculateDistance(user.first, user.second, unlockLat, unlockLon)
            }
            val isWithinRadius = distance != null && distance <= LOCATION_UNLOCK_RADIUS_METERS
            if (!isWithinRadius) {
                showLocationLockedState(capsule, distance)
                return
            }
        }

        showUnlockedState(capsule)
    }

    // FIX: 3
    private fun isCapsuleUnlockedByGate(capsule: com.example.chronovault.data.local.entity.CapsuleEntity): Boolean {
        val unlockTime = capsule.unlockTime ?: 0L
        val now = System.currentTimeMillis()
        if (unlockTime > 0L && now < unlockTime) return false

        if (capsule.isLocationBased) {
            val unlockLat = capsule.unlockLatitude ?: capsule.latitude
            val unlockLon = capsule.unlockLongitude ?: capsule.longitude
            val distance = lastKnownUserLocation?.let { user ->
                LocationHelper.calculateDistance(user.first, user.second, unlockLat, unlockLon)
            }
            return distance != null && distance <= LOCATION_UNLOCK_RADIUS_METERS
        }

        return true
    }

    // FIX: 3
    private fun showUnlockedState(capsule: com.example.chronovault.data.local.entity.CapsuleEntity) {
        val transitionedFromLocked = wasUnlockedByGatePreviously == false && !unlockTransitionPlayed
        wasUnlockedByGatePreviously = true
        isCurrentlyLockedByGate = false
        stopCountdown()
        binding.apply {
            tvTitle.text = capsule.title
            tvDate.text = getString(R.string.label_created) + ": " + formatDate(capsule.createdAt)
            tvMessage.visibility = View.VISIBLE
            tvMessageCard.visibility = View.VISIBLE
            tvMessageLabel.visibility = View.VISIBLE
            ivCapsuleImage.visibility = View.VISIBLE
            tvLocation.visibility = View.VISIBLE
            layoutSharedWith.visibility = if (viewModel.isOwner.value == true) View.VISIBLE else View.GONE
            val showComments = viewModel.canComment.value == true
            layoutComments.visibility = if (showComments) View.VISIBLE else View.GONE
            commentInputLayout.visibility = if (showComments) View.VISIBLE else View.GONE
            btnShare.visibility = if (viewModel.isOwner.value == true) View.VISIBLE else View.GONE
            btnMakePrivate.visibility = if (viewModel.isOwner.value == true && viewModel.isSharedCapsule.value == true) View.VISIBLE else View.GONE
            tvUnlockReason.text = getString(R.string.status_unlocked)
        }

        if (transitionedFromLocked) {
            runUnlockTransitionAnimationOnce()
        }
    }

    // FIX: 3
    private fun showTimeLockedState(capsule: com.example.chronovault.data.local.entity.CapsuleEntity, unlockTime: Long) {
        wasUnlockedByGatePreviously = false
        hideLockedContent()
        binding.tvTitle.text = capsule.title
        binding.tvDate.text = getString(R.string.label_created) + ": " + formatDate(capsule.createdAt)
        startCountdown(unlockTime)
    }

    // FIX: 3
    private fun showLocationLockedState(capsule: com.example.chronovault.data.local.entity.CapsuleEntity, distance: Float?) {
        wasUnlockedByGatePreviously = false
        stopCountdown()
        hideLockedContent()
        binding.tvTitle.text = capsule.title
        binding.tvDate.text = getString(R.string.label_created) + ": " + formatDate(capsule.createdAt)
        val distanceText = distance?.roundToInt()?.let { "$it m" } ?: "unknown"
        binding.tvUnlockReason.text = "Visit the saved location to unlock this memory\nYou are $distanceText away."
    }

    // FIX: 3
    private fun hideLockedContent() {
        isCurrentlyLockedByGate = true
        binding.apply {
            tvMessage.visibility = View.GONE
            tvMessageCard.visibility = View.GONE
            tvMessageLabel.visibility = View.GONE
            ivCapsuleImage.visibility = View.GONE
            tvLocation.visibility = View.GONE
            layoutSharedWith.visibility = View.GONE
            val showComments = viewModel.canComment.value == true
            layoutComments.visibility = if (showComments) View.VISIBLE else View.GONE
            commentInputLayout.visibility = if (showComments) View.VISIBLE else View.GONE
            btnShare.visibility = View.GONE
            btnMakePrivate.visibility = View.GONE
        }
    }

    private fun runUnlockTransitionAnimationOnce() {
        unlockTransitionPlayed = true
        val revealViews = listOf(binding.tvMessageCard, binding.ivCapsuleImage, binding.tvMessage)
        revealViews.forEach { view ->
            view.alpha = 0f
            view.scaleX = 0.95f
            view.scaleY = 0.95f
            view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(300L)
                .start()
        }
        vibrateUnlockFeedback()
    }

    private fun vibrateUnlockFeedback() {
        runCatching {
            val durationMs = 80L
            val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VibratorManager::class.java)
                vibratorManager?.defaultVibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(effect)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_UNLOCK_TRANSITION_PLAYED, unlockTransitionPlayed)
    }

    // FIX: 3
    private fun startCountdown(unlockTime: Long) {
        stopCountdown()
        countdownRunnable = object : Runnable {
            override fun run() {
                val remaining = unlockTime - System.currentTimeMillis()
                if (remaining <= 0L) {
                    binding.tvUnlockReason.text = getString(R.string.status_unlocked)
                    viewModel.capsule.value?.let { bindCapsuleData(it) }
                    return
                }

                val totalSeconds = remaining / 1000
                binding.tvUnlockReason.text = "Unlocks in ${CountdownFormatter.formatRemainingDuration(totalSeconds)}"
                mainHandler.postDelayed(this, 1000L)
            }
        }
        mainHandler.post(countdownRunnable!!)
    }

    // FIX: 3
    private fun stopCountdown() {
        countdownRunnable?.let { mainHandler.removeCallbacks(it) }
        countdownRunnable = null
    }

    // FIX: 3
    private fun fetchLastKnownLocation() {
        val hasFineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation && !hasCoarseLocation) return
        if (!GooglePlayServicesGuard.warnIfUnavailable(this, "CapsuleDetails")) return

        try {
            LocationServices.getFusedLocationProviderClient(this)
                .lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        lastKnownUserLocation = it.latitude to it.longitude
                        viewModel.capsule.value?.let { capsule -> bindCapsuleData(capsule) }
                    }
                }
                .addOnFailureListener { throwable ->
                    Log.w("CapsuleDetails", "Failed to fetch last known location", throwable)
                }
        } catch (securityException: SecurityException) {
            Log.w("CapsuleDetails", "Security exception while reading fused location", securityException)
        }
    }

    private fun handleActionState(state: ActionState) {
        when (state) {
            is ActionState.Loading -> {
                binding.progressDetails.visibility = View.VISIBLE
            }
            is ActionState.Success -> {
                binding.progressDetails.visibility = View.GONE
                Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
            }
            ActionState.CapsuleDeleted -> {
                Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show()
                finish()
            }
            is ActionState.Error -> {
                binding.progressDetails.visibility = View.GONE
                Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
            }
            ActionState.Idle -> {}
        }
        // FIX: unlocked-memory-crash
        // Avoid re-emitting Idle from an already-Idle state, which can loop callbacks.
        if (state !is ActionState.Idle) {
            viewModel.resetActionState()
        }
    }

    private fun showShareDialog() {
        val editText = EditText(this).apply {
            hint = getString(R.string.share_dialog_hint_user_id)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.share_dialog_title)
            .setView(editText)
            .setPositiveButton(R.string.button_share) { _, _ ->
                val userId = editText.text.toString().trim()
                if (userId.isNotEmpty()) {
                    viewModel.shareCapsule(userId)
                } else {
                    Toast.makeText(this, R.string.error_required_field, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.dismiss, null)
            .show()
    }

    private fun showMakePrivateConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_make_private_title)
            .setMessage(R.string.confirm_make_private_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                viewModel.makeCapsulePrivate()
            }
            .setNegativeButton(R.string.dismiss, null)
            .show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.button_delete) { _, _ ->
                viewModel.deleteCapsule()
            }
            .setNegativeButton(R.string.dismiss, null)
            .show()
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    override fun onDestroy() {
        stopCountdown()
        super.onDestroy()
    }

    companion object {
        // FIX: 15
        private const val LOCATION_UNLOCK_RADIUS_METERS = 50f
        private const val KEY_UNLOCK_TRANSITION_PLAYED = "key_unlock_transition_played"
    }
}
