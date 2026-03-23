package com.example.chronovault.ui.capsules

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chronovault.R
import com.example.chronovault.databinding.ActivityCapsuleDetailsBinding
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.ImageConverter
import com.example.chronovault.utils.LocationHelper
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCapsuleDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FIX: 3
        capsuleId = intent.getStringExtra("capsule_id")
        capsuleId?.let { viewModel.loadCapsule(it) }
        fetchLastKnownLocation()

        setupAdapters()
        setupUI()
        observeViewModel()
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
            onRemoveClick = { email -> viewModel.unshareCapsule(email) }
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
                showShareDialog()
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                    // Show comment input for shared capsules (both owner and shared users can comment)
                    binding.commentInputLayout.visibility = if (isShared && !isCurrentlyLockedByGate) View.VISIBLE else View.GONE
                    binding.layoutComments.visibility = if (isShared && !isCurrentlyLockedByGate) View.VISIBLE else View.GONE
                }

                viewModel.unlockReason.observe(this@CapsuleDetailsActivity) { reason ->
                    binding.tvUnlockReason.text = reason
                }

                viewModel.sharedWithEmails.observe(this@CapsuleDetailsActivity) { emails ->
                    sharedWithAdapter.submitList(emails.toList())
                    binding.tvNoShares.visibility = if (emails.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvSharedWith.visibility = if (emails.isNotEmpty()) View.VISIBLE else View.GONE
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

            capsule.imageBase64?.let { base64 ->
                val bitmap = ImageConverter.base64ToBitmap(base64)
                bitmap?.let { ivCapsuleImage.setImageBitmap(it) }
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
            layoutComments.visibility = if (viewModel.isSharedCapsule.value == true) View.VISIBLE else View.GONE
            commentInputLayout.visibility = if (viewModel.isSharedCapsule.value == true) View.VISIBLE else View.GONE
            btnShare.visibility = if (viewModel.isOwner.value == true) View.VISIBLE else View.GONE
            btnMakePrivate.visibility = if (viewModel.isOwner.value == true && viewModel.isSharedCapsule.value == true) View.VISIBLE else View.GONE
            tvUnlockReason.text = getString(R.string.status_unlocked)
        }
    }

    // FIX: 3
    private fun showTimeLockedState(capsule: com.example.chronovault.data.local.entity.CapsuleEntity, unlockTime: Long) {
        hideLockedContent()
        binding.tvTitle.text = capsule.title
        binding.tvDate.text = getString(R.string.label_created) + ": " + formatDate(capsule.createdAt)
        startCountdown(unlockTime)
    }

    // FIX: 3
    private fun showLocationLockedState(capsule: com.example.chronovault.data.local.entity.CapsuleEntity, distance: Float?) {
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
            layoutComments.visibility = View.GONE
            commentInputLayout.visibility = View.GONE
            btnShare.visibility = View.GONE
            btnMakePrivate.visibility = View.GONE
        }
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

                // FIX: 9
                // Details page countdown is minute-granularity (dashboard keeps seconds).
                val totalSeconds = remaining / 1000
                val days = totalSeconds / 86_400
                val hours = (totalSeconds % 86_400) / 3_600
                val minutes = (totalSeconds % 3_600) / 60
                binding.tvUnlockReason.text = "Unlocks in ${days}d ${hours}h ${minutes}m"
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

        LocationServices.getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    lastKnownUserLocation = it.latitude to it.longitude
                    viewModel.capsule.value?.let { capsule -> bindCapsuleData(capsule) }
                }
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
        viewModel.resetActionState()
    }

    private fun showShareDialog() {
        val editText = EditText(this).apply {
            hint = getString(R.string.share_dialog_hint)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.share_dialog_title)
            .setView(editText)
            .setPositiveButton(R.string.button_share) { _, _ ->
                val email = editText.text.toString().trim()
                if (email.isNotEmpty() && email.contains("@")) {
                    viewModel.shareCapsule(email)
                } else {
                    Toast.makeText(this, R.string.error_invalid_email, Toast.LENGTH_SHORT).show()
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
        private const val LOCATION_UNLOCK_RADIUS_METERS = 100f
    }
}
