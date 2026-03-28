package com.example.chronovault.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chronovault.R
import com.example.chronovault.data.local.entity.NotificationCategory
import com.example.chronovault.databinding.FragmentNotificationsBinding
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.ui.chat.ChatFragment
import com.example.chronovault.ui.home.HomeFragment
import com.example.chronovault.ui.map.MapFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * NotificationsFragment - Display app notifications
 * Shows unlock events, sharing notifications, etc.
 */
class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var adapter: NotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupUI()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(
            onNotificationClick = { notification ->
                viewModel.markAsRead(notification.id)
                handleNotificationTap(notification)
            },
            onDeleteClick = { notification ->
                viewModel.deleteNotification(notification.id)
            }
        )

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@NotificationsFragment.adapter
        }
    }

    private fun setupUI() {
        binding.chipPersonal.setOnClickListener {
            viewModel.setCategory(NotificationCategory.PERSONAL)
        }

        binding.chipWorld.setOnClickListener {
            viewModel.setCategory(NotificationCategory.WORLD)
        }

        binding.btnClearAll.setOnClickListener {
            viewModel.clearAllNotifications()
        }
    }

    private fun observeViewModel() {
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            adapter.submitList(notifications)
            binding.layoutNoNotifications.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.emptyStateMessage.observe(viewLifecycleOwner) { emptyMessage ->
            binding.tvNoNotifications.text = emptyMessage
        }

        viewModel.selectedCategory.observe(viewLifecycleOwner) { selected ->
            binding.chipGroupNotifications.check(
                if (selected == NotificationCategory.PERSONAL) R.id.chipPersonal else R.id.chipWorld
            )
        }

        viewModel.loadingState.observe(viewLifecycleOwner) { state ->
            when (state) {
                LoadingState.Loading -> binding.progressNotifications.visibility = View.VISIBLE
                LoadingState.Success -> binding.progressNotifications.visibility = View.GONE
                is LoadingState.Error -> {
                    binding.progressNotifications.visibility = View.GONE
                    showError(state.message)
                }
                LoadingState.Idle -> {}
            }
        }
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun handleNotificationTap(notification: AppNotification) {
        if (!isAdded) return
        when (notification.type) {
            com.example.chronovault.data.local.entity.NotificationType.NEARBY -> {
                parentFragmentManager.setFragmentResult(
                    MapFragment.MAP_FOCUS_REQUEST,
                    bundleOf(MapFragment.KEY_FOCUS_CAPSULE_ID to (notification.capsuleId ?: ""))
                )
                requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId = R.id.navigation_map
            }
            com.example.chronovault.data.local.entity.NotificationType.CHAT -> {
                val payload = notification.capsuleId.orEmpty()
                val parts = payload.split("|")
                val chatId = parts.getOrNull(0).orEmpty()
                val otherUserId = parts.getOrNull(1).orEmpty()
                if (chatId.isBlank() || otherUserId.isBlank()) return

                findNavController().navigate(
                    R.id.chatFragment,
                    bundleOf(
                        ChatFragment.ARG_CHAT_ID to chatId,
                        ChatFragment.ARG_OTHER_USER_ID to otherUserId
                    )
                )
            }
            else -> {
                val capsuleId = notification.capsuleId ?: return
                parentFragmentManager.setFragmentResult(
                    HomeFragment.CAPSULE_OPEN_REQUEST,
                    bundleOf(HomeFragment.KEY_CAPSULE_ID to capsuleId)
                )
                requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId = R.id.navigation_capsules
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}