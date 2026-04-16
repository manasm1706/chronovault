package com.example.chronovault.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.R
import com.example.chronovault.databinding.ItemChatSummaryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val currentUserId: String,
    private val onChatClick: (ChatSummary) -> Unit
) : ListAdapter<ChatSummary, ChatListAdapter.ChatViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding, currentUserId, onChatClick)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatViewHolder(
        private val binding: ItemChatSummaryBinding,
        private val currentUserId: String,
        private val onChatClick: (ChatSummary) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatSummary) {
            val displayMessage = chat.lastMessage.ifBlank { itemView.context.getString(R.string.chat_no_messages) }
            binding.tvFriendName.text = chat.otherUserName.ifBlank { chat.otherUserId }
            binding.tvLastMessage.text = if (chat.lastSenderName.isNotBlank() && chat.lastMessage.isNotBlank()) {
                "${chat.lastSenderName}: $displayMessage"
            } else {
                displayMessage
            }
            binding.tvChatTime.text = if (chat.lastTimestamp > 0L) {
                SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(chat.lastTimestamp))
            } else {
                ""
            }

            val hasUnread = chat.lastSenderId.isNotBlank() && chat.lastSenderId != currentUserId
            binding.tvUnreadBadge.visibility = if (hasUnread) View.VISIBLE else View.GONE
            binding.tvUnreadBadge.text = ""
            binding.root.setOnClickListener { onChatClick(chat) }
        }
    }

    class Diff : DiffUtil.ItemCallback<ChatSummary>() {
        override fun areItemsTheSame(oldItem: ChatSummary, newItem: ChatSummary): Boolean = oldItem.chatId == newItem.chatId
        override fun areContentsTheSame(oldItem: ChatSummary, newItem: ChatSummary): Boolean = oldItem == newItem
    }
}

