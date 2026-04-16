package com.example.chronovault.ui.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
        parentFragmentManager.setFragmentResultListener(NewChatBottomSheet.RESULT_KEY, viewLifecycleOwner) { _, bundle ->
            val userId = bundle.getString(NewChatBottomSheet.RESULT_USER_ID).orEmpty()
            if (userId.isNotBlank()) {
                viewModel.startChat(userId)
            }
        }

        adapter = ChatListAdapter(
            currentUserId = viewModel.getCurrentUserId(),
            onChatClick = { chat -> viewModel.openChat(chat) }
        )
        binding.rvChats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChats.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
        })

        binding.fabNewChat.setOnClickListener {
            NewChatBottomSheet().show(parentFragmentManager, "new_chat_sheet")
        }
        binding.btnEmptyNewChat.setOnClickListener {
            NewChatBottomSheet().show(parentFragmentManager, "new_chat_sheet")
        }

        viewModel.filteredChats.observe(viewLifecycleOwner) { chats ->
            adapter.submitList(chats)
            val shouldShowEmpty = chats.isEmpty() && binding.etSearch.text?.isBlank() == true
            binding.layoutEmptyChats.visibility = if (shouldShowEmpty) View.VISIBLE else View.GONE
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
            if (state is LoadingState.Error) {
                val uiMessage = if (state.message.contains("permission", ignoreCase = true)) {
                    "Chat sync failed. Check permissions."
                } else {
                    state.message
                }
                Toast.makeText(requireContext(), uiMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

