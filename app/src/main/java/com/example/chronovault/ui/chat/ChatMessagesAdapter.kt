package com.example.chronovault.ui.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.R
import com.example.chronovault.databinding.ItemChatMessageBinding
import com.google.android.material.color.MaterialColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatMessagesAdapter(
    private val currentUserId: String,
    private val onViewCapsule: (String) -> Unit
) : ListAdapter<ChatMessage, ChatMessagesAdapter.MessageViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding, currentUserId, onViewCapsule)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(
        private val binding: ItemChatMessageBinding,
        private val currentUserId: String,
        private val onViewCapsule: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            val isSender = message.senderId == currentUserId
            val params = binding.cardMessage.layoutParams as ViewGroup.MarginLayoutParams
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT
            if (params is androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
                params.horizontalBias = if (isSender) 1f else 0f
            }
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

            binding.tvMessageText.text = message.text
            binding.tvMessageTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

            val isCapsule = message.type == ChatMessageType.CAPSULE && !message.capsuleId.isNullOrBlank()
            binding.layoutCapsule.visibility = if (isCapsule) View.VISIBLE else View.GONE
            if (isCapsule) {
                binding.tvCapsuleTitle.text = message.text
                binding.btnViewCapsule.setOnClickListener {
                    message.capsuleId?.let(onViewCapsule)
                }
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean = oldItem.messageId == newItem.messageId
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean = oldItem == newItem
    }
}

