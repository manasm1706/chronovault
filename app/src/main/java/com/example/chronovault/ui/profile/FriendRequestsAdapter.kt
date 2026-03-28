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
) : ListAdapter<FriendRepository.FriendRequest, FriendRequestsAdapter.RequestViewHolder>(Diff()) {

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

        fun bind(request: FriendRepository.FriendRequest) {
            binding.tvRequestUserId.text = request.fromUserId
            binding.btnAcceptRequest.setOnClickListener { onAccept(request) }
            binding.btnRejectRequest.setOnClickListener { onReject(request) }
        }
    }

    class Diff : DiffUtil.ItemCallback<FriendRepository.FriendRequest>() {
        override fun areItemsTheSame(
            oldItem: FriendRepository.FriendRequest,
            newItem: FriendRepository.FriendRequest
        ): Boolean = oldItem.requestId == newItem.requestId

        override fun areContentsTheSame(
            oldItem: FriendRepository.FriendRequest,
            newItem: FriendRepository.FriendRequest
        ): Boolean = oldItem == newItem
    }
}

