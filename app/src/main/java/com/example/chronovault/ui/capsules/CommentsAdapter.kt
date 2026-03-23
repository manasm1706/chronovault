package com.example.chronovault.ui.capsules

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.data.local.entity.CommentEntity
import com.example.chronovault.databinding.ItemCommentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for displaying comments on a capsule
 */
class CommentsAdapter(
    private val currentUserId: String,
    private val onDeleteClick: (CommentEntity) -> Unit
) : ListAdapter<CommentEntity, CommentsAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding, currentUserId, onDeleteClick)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentViewHolder(
        private val binding: ItemCommentBinding,
        private val currentUserId: String,
        private val onDeleteClick: (CommentEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: CommentEntity) {
            binding.apply {
                tvCommentAuthor.text = comment.authorName
                tvCommentText.text = comment.text
                tvCommentTime.text = formatTime(comment.createdAt)

                // Only show delete button for own comments
                if (comment.authorId == currentUserId) {
                    btnDeleteComment.visibility = View.VISIBLE
                    btnDeleteComment.setOnClickListener { onDeleteClick(comment) }
                } else {
                    btnDeleteComment.visibility = View.GONE
                }
            }
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            return when {
                diff < 60_000 -> "just now"
                diff < 3_600_000 -> "${diff / 60_000}m ago"
                diff < 86_400_000 -> "${diff / 3_600_000}h ago"
                else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<CommentEntity>() {
        override fun areItemsTheSame(oldItem: CommentEntity, newItem: CommentEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CommentEntity, newItem: CommentEntity): Boolean {
            return oldItem == newItem
        }
    }
}

