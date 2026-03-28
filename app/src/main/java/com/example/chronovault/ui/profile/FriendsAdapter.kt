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
) : ListAdapter<FriendEntity, FriendsAdapter.FriendViewHolder>(DiffCallback()) {

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

        fun bind(friend: FriendEntity) {
            binding.tvFriendId.text = friend.friendUserId
            binding.tvFriendStatus.text = friend.status.name
            val showAccept = friend.status == FriendStatus.PENDING
            binding.btnAcceptFriend.visibility = if (showAccept) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnAcceptFriend.setOnClickListener { onAcceptClick(friend) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FriendEntity>() {
        override fun areItemsTheSame(oldItem: FriendEntity, newItem: FriendEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FriendEntity, newItem: FriendEntity): Boolean = oldItem == newItem
    }
}

