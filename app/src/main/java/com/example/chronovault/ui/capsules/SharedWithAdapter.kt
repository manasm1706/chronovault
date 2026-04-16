package com.example.chronovault.ui.capsules

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.databinding.ItemSharedWithBinding

/**
 * RecyclerView adapter for displaying shared-with email list
 * Allows owner to remove individual shares
 */
class SharedWithAdapter(
    private val onRemoveClick: (SharedUserItem) -> Unit
) : ListAdapter<SharedUserItem, SharedWithAdapter.SharedWithViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharedWithViewHolder {
        val binding = ItemSharedWithBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SharedWithViewHolder(binding, onRemoveClick)
    }

    override fun onBindViewHolder(holder: SharedWithViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SharedWithViewHolder(
        private val binding: ItemSharedWithBinding,
        private val onRemoveClick: (SharedUserItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: SharedUserItem) {
            binding.tvSharedEmail.text = user.displayName
            binding.tvSharedSubtitle.text = user.subtitle
            binding.btnRemoveShare.setOnClickListener { onRemoveClick(user) }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<SharedUserItem>() {
        override fun areItemsTheSame(oldItem: SharedUserItem, newItem: SharedUserItem) = oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: SharedUserItem, newItem: SharedUserItem) = oldItem == newItem
    }
}

data class SharedUserItem(
    val userId: String,
    val displayName: String,
    val subtitle: String
)

