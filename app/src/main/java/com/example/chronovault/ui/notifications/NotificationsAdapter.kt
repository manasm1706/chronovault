package com.example.chronovault.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.R
import com.example.chronovault.data.local.entity.NotificationType
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
                    NotificationType.UNLOCK -> R.drawable.ic_asset_bookmark
                    NotificationType.NEARBY -> R.drawable.ic_asset_map
                    NotificationType.SHARE -> R.drawable.ic_asset_contacts
                    NotificationType.CHAT -> R.drawable.ic_asset_contacts
                }
                ivIcon.setImageResource(iconRes)

                // Read state
                cardNotification.alpha = if (notification.read) 0.65f else 1f
                cardNotification.scaleX = 0.98f
                cardNotification.scaleY = 0.98f
                cardNotification.animate().scaleX(1f).scaleY(1f).setDuration(120L).start()

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

