package com.example.chronovault.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.data.repository.FriendRepository
import com.example.chronovault.databinding.ItemFriendRequestBinding

class FriendRequestsAdapter(
    private val onAccept: (FriendRepository.FriendRequest) -> Unit,
    private val onReject: (FriendRepository.FriendRequest) -> Unit
) : ListAdapter<FriendRequestDisplayItem, FriendRequestsAdapter.RequestViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemFriendRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding, onAccept, onReject)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RequestViewHolder(
        private val binding: ItemFriendRequestBinding,
        private val onAccept: (FriendRepository.FriendRequest) -> Unit,
        private val onReject: (FriendRepository.FriendRequest) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FriendRequestDisplayItem) {
            binding.tvRequestUserId.text = item.name
            binding.tvRequestSubtitle.text = item.subtitle
            binding.btnAcceptRequest.setOnClickListener { onAccept(item.request) }
            binding.btnRejectRequest.setOnClickListener { onReject(item.request) }
        }
    }

    class Diff : DiffUtil.ItemCallback<FriendRequestDisplayItem>() {
        override fun areItemsTheSame(
            oldItem: FriendRequestDisplayItem,
            newItem: FriendRequestDisplayItem
        ): Boolean = oldItem.request.requestId == newItem.request.requestId

        override fun areContentsTheSame(
            oldItem: FriendRequestDisplayItem,
            newItem: FriendRequestDisplayItem
        ): Boolean = oldItem == newItem
    }
}

