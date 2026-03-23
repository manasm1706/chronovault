package com.example.chronovault.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chronovault.databinding.FragmentNotificationsBinding
import com.example.chronovault.ui.common.LoadingState
import kotlinx.coroutines.launch

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
        binding.btnClearAll.setOnClickListener {
            viewModel.clearAllNotifications()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
                    adapter.submitList(notifications)
                    binding.tvNoNotifications.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
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
        }
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}