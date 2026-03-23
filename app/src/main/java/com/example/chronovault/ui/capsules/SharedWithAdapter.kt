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
    private val onRemoveClick: (String) -> Unit
) : ListAdapter<String, SharedWithAdapter.SharedWithViewHolder>(StringDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharedWithViewHolder {
        val binding = ItemSharedWithBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SharedWithViewHolder(binding, onRemoveClick)
    }

    override fun onBindViewHolder(holder: SharedWithViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SharedWithViewHolder(
        private val binding: ItemSharedWithBinding,
        private val onRemoveClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(email: String) {
            binding.tvSharedEmail.text = email
            binding.btnRemoveShare.setOnClickListener { onRemoveClick(email) }
        }
    }

    class StringDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}

