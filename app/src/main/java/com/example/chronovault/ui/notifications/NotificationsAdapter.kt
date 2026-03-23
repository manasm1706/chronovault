package com.example.chronovault.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.R
import com.example.chronovault.databinding.ItemNotificationBinding

/**
 * RecyclerView adapter for displaying notifications
 */
class NotificationsAdapter(
    private val onNotificationClick: (AppNotification) -> Unit,
    private val onDeleteClick: (AppNotification) -> Unit
) : ListAdapter<AppNotification, NotificationsAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding, onNotificationClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        private val binding: ItemNotificationBinding,
        private val onNotificationClick: (AppNotification) -> Unit,
        private val onDeleteClick: (AppNotification) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: AppNotification) {
            binding.apply {
                tvTitle.text = notification.title
                tvMessage.text = notification.message
                tvTime.text = getTimeAgo(notification.timestamp)

                // Icon based on type
                val iconRes = when (notification.type) {
                    NotificationType.TIME_UNLOCK -> R.drawable.ic_asset_bookmark
                    NotificationType.LOCATION_UNLOCK -> R.drawable.ic_asset_map
                    NotificationType.SHARED -> R.drawable.ic_asset_contacts
                    NotificationType.CAPSULE_CREATED -> R.drawable.ic_asset_cog
                    else -> R.drawable.ic_asset_bookmark
                }
                ivIcon.setImageResource(iconRes)

                // Read state
                if (notification.read) {
                    cardNotification.alpha = 0.6f
                }

                cardNotification.setOnClickListener {
                    onNotificationClick(notification)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(notification)
                }
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diffInSeconds = (now - timestamp) / 1000

            return when {
                diffInSeconds < 60 -> "just now"
                diffInSeconds < 3600 -> "${diffInSeconds / 60}m ago"
                diffInSeconds < 86400 -> "${diffInSeconds / 3600}h ago"
                else -> "${diffInSeconds / 86400}d ago"
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean {
            return oldItem == newItem
        }
    }
}

