package com.example.chronovault.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chronovault.R
import com.example.chronovault.databinding.FragmentChatListBinding
import com.example.chronovault.ui.common.LoadingState

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatListViewModel by viewModels()
    private lateinit var adapter: ChatListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ChatListAdapter { chat -> viewModel.openChat(chat) }
        binding.rvChats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChats.adapter = adapter

        viewModel.chats.observe(viewLifecycleOwner) { chats ->
            adapter.submitList(chats)
            binding.tvEmptyChats.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.openChatEvent.observe(viewLifecycleOwner) { chat ->
            if (chat == null) return@observe
            findNavController().navigate(
                R.id.action_chatListFragment_to_chatFragment,
                Bundle().apply {
                    putString(ChatFragment.ARG_OTHER_USER_ID, chat.otherUserId)
                    putString(ChatFragment.ARG_CHAT_ID, chat.chatId)
                }
            )
            viewModel.consumeOpenChatEvent()
        }

        viewModel.loadingState.observe(viewLifecycleOwner) { state ->
            binding.progressChats.visibility = if (state == LoadingState.Loading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

