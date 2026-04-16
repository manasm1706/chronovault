package com.example.chronovault.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.databinding.ItemUserSearchSelectableBinding

class UserSearchSelectableAdapter(
    private val multiSelect: Boolean,
    private val onUserTapped: (UserSearchItem) -> Unit,
    private val isSelected: (String) -> Boolean
) : ListAdapter<UserSearchItem, UserSearchSelectableAdapter.UserViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserSearchSelectableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: ItemUserSearchSelectableBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserSearchItem) {
            binding.tvUserName.text = item.name
            binding.tvUserSubtitle.text = item.email.ifBlank { item.id }
            val selected = isSelected(item.id)
            binding.checkboxSelect.visibility = if (multiSelect) View.VISIBLE else View.GONE
            binding.checkboxSelect.isChecked = selected
            binding.root.setOnClickListener { onUserTapped(item) }
        }
    }

    class Diff : DiffUtil.ItemCallback<UserSearchItem>() {
        override fun areItemsTheSame(oldItem: UserSearchItem, newItem: UserSearchItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: UserSearchItem, newItem: UserSearchItem): Boolean = oldItem == newItem
    }
}

