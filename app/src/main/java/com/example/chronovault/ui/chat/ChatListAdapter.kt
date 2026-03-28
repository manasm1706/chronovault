package com.example.chronovault.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.databinding.ItemChatSummaryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val onChatClick: (ChatSummary) -> Unit
) : ListAdapter<ChatSummary, ChatListAdapter.ChatViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding, onChatClick)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatViewHolder(
        private val binding: ItemChatSummaryBinding,
        private val onChatClick: (ChatSummary) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatSummary) {
            binding.tvFriendName.text = chat.otherUserId
            binding.tvLastMessage.text = chat.lastMessage.ifBlank { "No messages yet" }
            binding.tvChatTime.text = if (chat.lastTimestamp > 0L) {
                SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(chat.lastTimestamp))
            } else {
                ""
            }
            binding.root.setOnClickListener { onChatClick(chat) }
        }
    }

    class Diff : DiffUtil.ItemCallback<ChatSummary>() {
        override fun areItemsTheSame(oldItem: ChatSummary, newItem: ChatSummary): Boolean = oldItem.chatId == newItem.chatId
        override fun areContentsTheSame(oldItem: ChatSummary, newItem: ChatSummary): Boolean = oldItem == newItem
    }
}

