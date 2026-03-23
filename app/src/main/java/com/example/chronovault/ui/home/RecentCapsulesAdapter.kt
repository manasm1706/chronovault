package com.example.chronovault.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.R
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.databinding.ItemRecentCapsuleBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for the Home dashboard's recent memory list.
 */
class RecentCapsulesAdapter(
    private val onCapsuleClick: (CapsuleEntity) -> Unit
) : ListAdapter<CapsuleEntity, RecentCapsulesAdapter.RecentCapsuleViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentCapsuleViewHolder {
        val binding = ItemRecentCapsuleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentCapsuleViewHolder(binding, onCapsuleClick)
    }

    override fun onBindViewHolder(holder: RecentCapsuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecentCapsuleViewHolder(
        private val binding: ItemRecentCapsuleBinding,
        private val onCapsuleClick: (CapsuleEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(capsule: CapsuleEntity) {
            binding.tvRecentTitle.text = capsule.title
            binding.tvRecentDate.text = binding.root.context.getString(
                R.string.home_recent_date,
                formatDate(capsule.createdAt)
            )

            // Improved visual lock status indicators
            binding.tvRecentStatus.text = when {
                capsule.isUnlocked -> "✅ Unlocked"
                capsule.isTimeBased -> "⏰ Time-locked"
                capsule.isLocationBased -> "📍 Location-locked"
                else -> "🔒 Locked"
            }

            binding.ivRecentLocation.visibility = if (capsule.latitude != 0.0 || capsule.longitude != 0.0) {
                View.VISIBLE
            } else {
                View.GONE
            }

            binding.root.setOnClickListener {
                if (capsule.id.isBlank()) return@setOnClickListener
                // FIX: 14
                // Keep adapter click lightweight; delegate access logic to the fragment.
                onCapsuleClick(capsule)
            }
        }

        private fun formatDate(timestamp: Long): String {
            return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CapsuleEntity>() {
        override fun areItemsTheSame(oldItem: CapsuleEntity, newItem: CapsuleEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CapsuleEntity, newItem: CapsuleEntity): Boolean {
            return oldItem == newItem
        }
    }
}

