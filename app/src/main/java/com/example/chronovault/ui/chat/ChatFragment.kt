package com.example.chronovault.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chronovault.R
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.databinding.FragmentChatBinding
import com.example.chronovault.ui.capsules.CapsuleDetailsActivity
import com.example.chronovault.ui.common.LoadingState

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatMessagesAdapter

    private var otherUserId: String = ""
    private var chatIdArg: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        otherUserId = arguments?.getString(ARG_OTHER_USER_ID).orEmpty()
        chatIdArg = arguments?.getString(ARG_CHAT_ID).orEmpty()
        if (otherUserId.isBlank() && chatIdArg.isBlank()) {
            binding.tvEmptyMessages.visibility = View.VISIBLE
            return
        }

        if (otherUserId.isNotBlank()) {
            binding.tvChatTitle.text = otherUserId
        }

        adapter = ChatMessagesAdapter(
            currentUserId = viewModel.getCurrentUserId(),
            onViewCapsule = { capsuleId ->
                startActivity(Intent(requireContext(), CapsuleDetailsActivity::class.java).apply {
                    putExtra("capsule_id", capsuleId)
                })
            }
        )
        val lm = LinearLayoutManager(requireContext())
        binding.rvMessages.layoutManager = lm
        binding.rvMessages.adapter = adapter

        binding.rvMessages.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(-1)) {
                    viewModel.loadMore()
                }
            }
        })

        binding.btnSendMessage.setOnClickListener {
            val text = binding.etMessage.text?.toString().orEmpty()
            viewModel.sendText(text)
            binding.etMessage.setText("")
        }

        binding.btnShareCapsule.setOnClickListener {
            viewModel.loadShareableCapsules()
        }

        viewModel.shareableCapsules.observe(viewLifecycleOwner) { capsules ->
            if (capsules.isEmpty()) return@observe
            showShareCapsuleDialog(capsules)
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            binding.tvEmptyMessages.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
            if (messages.isNotEmpty()) {
                binding.rvMessages.scrollToPosition(messages.lastIndex)
            }
        }

        viewModel.loadingState.observe(viewLifecycleOwner) { state ->
            binding.progressChat.visibility = if (state == LoadingState.Loading) View.VISIBLE else View.GONE
        }

        viewModel.startChat(otherUserId = otherUserId, chatId = chatIdArg)
    }

    override fun onResume() {
        super.onResume()
        val chatId = chatIdArg.ifBlank {
            val current = viewModel.getCurrentUserId()
            if (current.isBlank() || otherUserId.isBlank()) "" else "${listOf(current, otherUserId).sorted().joinToString("_")}"
        }
        if (chatId.isNotBlank()) {
            chatIdArg = chatId
            ChatSessionManager.activeChatId = chatId
        }
    }

    override fun onPause() {
        super.onPause()
        if (ChatSessionManager.activeChatId == chatIdArg) {
            ChatSessionManager.activeChatId = null
        }
    }

    private fun showShareCapsuleDialog(capsules: List<CapsuleEntity>) {
        val titles = capsules.map { it.title.ifBlank { it.id } }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.chat_share_capsule)
            .setItems(titles) { _, which ->
                val selected = capsules.getOrNull(which) ?: return@setItems
                viewModel.sendCapsule(selected.id, selected.title)
            }
            .setNegativeButton(R.string.dismiss, null)
            .show()
    }

    override fun onDestroyView() {
        viewModel.stopChat()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_OTHER_USER_ID = "arg_other_user_id"
        const val ARG_CHAT_ID = "arg_chat_id"
    }
}

