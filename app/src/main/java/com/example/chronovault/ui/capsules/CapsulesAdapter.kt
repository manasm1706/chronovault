package com.example.chronovault.ui.capsules

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.databinding.ItemCapsuleBinding
import com.example.chronovault.utils.ImageConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for displaying capsules
 */
class CapsulesAdapter(
    private val onCapsuleClick: (CapsuleEntity) -> Unit
) : ListAdapter<CapsuleEntity, CapsulesAdapter.CapsuleViewHolder>(CapsuleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CapsuleViewHolder {
        val binding = ItemCapsuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CapsuleViewHolder(binding, onCapsuleClick)
    }

    override fun onBindViewHolder(holder: CapsuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CapsuleViewHolder(
        private val binding: ItemCapsuleBinding,
        private val onCapsuleClick: (CapsuleEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(capsule: CapsuleEntity) {
            binding.apply {
                tvTitle.text = capsule.title
                tvLocation.text = "📍 Location saved"
                tvCreatedDate.text = formatDate(capsule.createdAt)

                // Improved visual lock status indicators
                tvStatus.text = when {
                    capsule.isUnlocked -> "✅ Unlocked"
                    capsule.isTimeBased -> "⏰ Time-locked"
                    capsule.isLocationBased -> "📍 Location-locked"
                    else -> "🔒 Locked"
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

