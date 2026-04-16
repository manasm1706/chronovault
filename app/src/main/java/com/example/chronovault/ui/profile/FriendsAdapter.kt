package com.example.chronovault.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.data.local.entity.FriendEntity
import com.example.chronovault.data.local.entity.FriendStatus
import com.example.chronovault.databinding.ItemFriendBinding

class FriendsAdapter(
    private val onAcceptClick: (FriendEntity) -> Unit
) : ListAdapter<FriendDisplayItem, FriendsAdapter.FriendViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendViewHolder(binding, onAcceptClick)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FriendViewHolder(
        private val binding: ItemFriendBinding,
        private val onAcceptClick: (FriendEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FriendDisplayItem) {
            binding.tvFriendId.text = item.name
            binding.tvFriendStatus.text = item.subtitle
            val showAccept = item.friend.status == FriendStatus.PENDING
            binding.btnAcceptFriend.visibility = if (showAccept) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnAcceptFriend.setOnClickListener { onAcceptClick(item.friend) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FriendDisplayItem>() {
        override fun areItemsTheSame(oldItem: FriendDisplayItem, newItem: FriendDisplayItem): Boolean = oldItem.friend.id == newItem.friend.id
        override fun areContentsTheSame(oldItem: FriendDisplayItem, newItem: FriendDisplayItem): Boolean = oldItem == newItem
    }
}

