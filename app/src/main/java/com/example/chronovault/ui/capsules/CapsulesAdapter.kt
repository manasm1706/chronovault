package com.example.chronovault.ui.capsules

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.R
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.databinding.ItemCapsuleBinding
import com.example.chronovault.utils.CountdownFormatter
import com.example.chronovault.utils.ImageConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for displaying capsules
 */
class CapsulesAdapter(
    private val onCapsuleClick: (CapsuleEntity) -> Unit,
    private val onMapClueClick: (CapsuleEntity) -> Unit
) : ListAdapter<CapsuleEntity, CapsulesAdapter.CapsuleViewHolder>(CapsuleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CapsuleViewHolder {
        val binding = ItemCapsuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CapsuleViewHolder(binding, onCapsuleClick, onMapClueClick)
    }

    override fun onBindViewHolder(holder: CapsuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: CapsuleViewHolder) {
        holder.unbind()
        super.onViewRecycled(holder)
    }

    class CapsuleViewHolder(
        private val binding: ItemCapsuleBinding,
        private val onCapsuleClick: (CapsuleEntity) -> Unit,
        private val onMapClueClick: (CapsuleEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val countdownHandler = Handler(Looper.getMainLooper())
        private var countdownRunnable: Runnable? = null

        fun bind(capsule: CapsuleEntity) {
            stopCountdown()

            binding.apply {
                val now = System.currentTimeMillis()
                val unlockTime = capsule.unlockTime ?: 0L
                val showTimeCountdown = capsule.isTimeBased && !capsule.isUnlocked && unlockTime > now
                val isLocationLocked = capsule.isLocationBased && !capsule.isUnlocked && !showTimeCountdown

                tvTitle.text = capsule.title
                tvLocation.text = itemView.context.getString(R.string.home_location_available)
                tvCreatedDate.text = formatDate(capsule.createdAt)
                ivMapClue.visibility = if (isLocationLocked) View.VISIBLE else View.GONE
                ivMapClue.setOnClickListener {
                    onMapClueClick(capsule)
                }

                if (showTimeCountdown) {
                    tvCountdown.visibility = View.VISIBLE
                    startCountdown(unlockTime)
                    tvStatus.apply {
                        text = itemView.context.getString(R.string.capsule_status_time_locked)
                    }
                } else {
                    tvCountdown.visibility = View.GONE
                    tvStatus.text = when {
                        capsule.isUnlocked || (capsule.isTimeBased && unlockTime in 1..now) -> itemView.context.getString(R.string.capsule_status_unlocked)
                        capsule.isSharedWithMe -> itemView.context.getString(R.string.capsule_status_shared)
                        isLocationLocked -> itemView.context.getString(R.string.capsule_status_locked)
                        else -> itemView.context.getString(R.string.capsule_status_locked)
                    }
                }

                // Set image if available
                capsule.imageBase64?.let { base64 ->
                    val bitmap = ImageConverter.base64ToBitmap(base64)
                    bitmap?.let { ivCapsuleImage.setImageBitmap(it) }
                }

                // FIX: 4
                // Delegate click handling to the fragment/viewmodel only.
                root.setOnClickListener {
                    onCapsuleClick(capsule)
                }
            }
        }

        fun unbind() {
            stopCountdown()
        }

        private fun startCountdown(unlockTime: Long) {
            val countdownText = binding.tvCountdown
            countdownRunnable = object : Runnable {
                override fun run() {
                    val remaining = unlockTime - System.currentTimeMillis()
                    if (remaining <= 0L) {
                        countdownText.visibility = View.GONE
                        binding.tvStatus.text = itemView.context.getString(R.string.capsule_status_unlocked)
                        return
                    }

                    countdownText.text = formatCountdown(remaining)
                    countdownHandler.postDelayed(this, 1000L)
                }
            }
            countdownHandler.post(countdownRunnable!!)
        }

        private fun stopCountdown() {
            countdownRunnable?.let { countdownHandler.removeCallbacks(it) }
            countdownRunnable = null
        }

        private fun formatCountdown(remainingMs: Long): String {
            val totalSeconds = remainingMs / 1000L
            return CountdownFormatter.formatRemainingDuration(totalSeconds)
        }

        private fun formatDate(timestamp: Long): String {
            return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    class CapsuleDiffCallback : DiffUtil.ItemCallback<CapsuleEntity>() {
        override fun areItemsTheSame(oldItem: CapsuleEntity, newItem: CapsuleEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CapsuleEntity, newItem: CapsuleEntity): Boolean {
            return oldItem == newItem
        }
    }
}

