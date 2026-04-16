package com.example.chronovault.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.data.local.OwnerCapsuleCommentItem
import com.example.chronovault.databinding.ItemMyCapsuleCommentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyCapsuleCommentsAdapter : ListAdapter<OwnerCapsuleCommentItem, MyCapsuleCommentsAdapter.CommentViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemMyCapsuleCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentViewHolder(private val binding: ItemMyCapsuleCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OwnerCapsuleCommentItem) {
            binding.tvCapsuleTitle.text = item.capsuleTitle
            binding.tvCommentText.text = item.text
            binding.tvCommentMeta.text = "${item.authorName} • ${formatDate(item.createdAt)}"
        }

        private fun formatDate(timestamp: Long): String {
            return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }

    class Diff : DiffUtil.ItemCallback<OwnerCapsuleCommentItem>() {
        override fun areItemsTheSame(oldItem: OwnerCapsuleCommentItem, newItem: OwnerCapsuleCommentItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OwnerCapsuleCommentItem, newItem: OwnerCapsuleCommentItem): Boolean {
            return oldItem == newItem
        }
    }
}

