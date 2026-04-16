package com.example.chronovault.ui.chat

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.databinding.ItemChatMessageBinding
import com.google.android.material.color.MaterialColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatMessagesAdapter(
    private val currentUserId: String,
    private val onViewCapsule: (String) -> Unit,
    private val onMessageLongPress: (ChatMessage) -> Unit
) : ListAdapter<ChatMessage, ChatMessagesAdapter.MessageViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding, currentUserId, onViewCapsule, onMessageLongPress)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(
        private val binding: ItemChatMessageBinding,
        private val currentUserId: String,
        private val onViewCapsule: (String) -> Unit,
        private val onMessageLongPress: (ChatMessage) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            Log.d("CHAT_DEBUG", "Rendering message: ${message.messageId} type=${message.type}")
            val isSender = message.senderId == currentUserId
            val params = binding.cardMessage.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.horizontalBias = if (isSender) 1f else 0f
            binding.cardMessage.layoutParams = params

            val background = if (isSender) {
                MaterialColors.getColor(binding.root, androidx.appcompat.R.attr.colorPrimary)
            } else {
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceVariant)
            }
            val textColor = if (isSender) {
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnPrimary)
            } else {
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface)
            }
            binding.cardMessage.setCardBackgroundColor(background)
            binding.tvMessageText.setTextColor(textColor)

            val isCapsule = message.type == ChatMessageType.CAPSULE && !message.capsuleId.isNullOrBlank()
            binding.tvMessageText.visibility = if (isCapsule) View.GONE else View.VISIBLE
            binding.tvMessageText.text = message.text
            binding.tvEditedLabel.visibility = if (message.isEdited) View.VISIBLE else View.GONE
            binding.tvDeletedLabel.visibility = if (message.isDeleted) View.VISIBLE else View.GONE
            binding.tvMessageTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
            binding.tvMessageTime.setTextColor(ColorUtils.setAlphaComponent(textColor, (255 * 0.65f).toInt()))
            binding.tvEditedLabel.setTextColor(ColorUtils.setAlphaComponent(textColor, (255 * 0.65f).toInt()))
            binding.tvDeletedLabel.setTextColor(ColorUtils.setAlphaComponent(textColor, (255 * 0.65f).toInt()))

            binding.layoutCapsule.visibility = if (isCapsule) View.VISIBLE else View.GONE
            if (isCapsule) {
                val normalizedTitle = message.text
                    .removePrefix("Shared capsule:")
                    .removePrefix("Shared Capsule:")
                    .trim()
                binding.tvCapsuleTitle.text = normalizedTitle.ifBlank { "Shared memory" }
                binding.tvCapsuleTitle.setTextColor(textColor)
                binding.tvCapsuleHeader.setTextColor(ColorUtils.setAlphaComponent(textColor, (255 * 0.75f).toInt()))
                binding.btnViewCapsule.setOnClickListener {
                    onViewCapsule(message.capsuleId)
                }
            } else {
                binding.tvCapsuleTitle.text = ""
                binding.tvCapsuleHeader.setTextColor(ColorUtils.setAlphaComponent(textColor, (255 * 0.75f).toInt()))
                binding.btnViewCapsule.setOnClickListener(null)
            }

            binding.root.setOnLongClickListener {
                if (isSender) {
                    onMessageLongPress(message)
                    true
                } else {
                    false
                }
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean = oldItem.messageId == newItem.messageId
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean = oldItem == newItem
    }
}

