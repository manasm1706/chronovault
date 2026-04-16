package com.example.chronovault.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chronovault.R
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.databinding.FragmentChatBinding
import com.example.chronovault.ui.capsules.CapsuleDetailsActivity
import com.example.chronovault.ui.common.LoadingState
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatMessagesAdapter
    private val userRepository by lazy { ServiceLocator.provideUserRepository(requireContext()) }

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

        binding.btnBack.setOnClickListener {
            val navController = findNavController()
            val navigatedUp = navController.navigateUp()
            if (!navigatedUp) {
                navController.popBackStack(R.id.chatListFragment, false)
                if (navController.currentDestination?.id != R.id.chatListFragment) {
                    navController.navigate(R.id.chatListFragment)
                }
            }
        }

        adapter = ChatMessagesAdapter(
            currentUserId = viewModel.getCurrentUserId(),
            onViewCapsule = { capsuleId ->
                startActivity(Intent(requireContext(), CapsuleDetailsActivity::class.java).apply {
                    putExtra("capsule_id", capsuleId)
                })
            },
            onMessageLongPress = { message -> showMessageActions(message) }
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

        resolveChatHeaderName()

        viewModel.startChat(otherUserId = otherUserId, chatId = chatIdArg)
    }

    private fun resolveChatHeaderName() {
        val targetUserId = otherUserId.ifBlank {
            chatIdArg.split("_").firstOrNull { it != viewModel.getCurrentUserId() }.orEmpty()
        }
        if (targetUserId.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val profile = userRepository.getUsersByIds(listOf(targetUserId)).getOrNull()?.firstOrNull()
            val name = profile?.get("name")?.toString().orEmpty().ifBlank { targetUserId }
            binding.tvChatTitle.text = name
            binding.tvChatSubtitle.text = profile?.get("email")?.toString().orEmpty().ifBlank { targetUserId }
        }
    }

    private fun showMessageActions(message: ChatMessage) {
        val options = mutableListOf<String>()
        val editable = message.senderId == viewModel.getCurrentUserId() && !message.isDeleted
        if (editable) options.add(getString(R.string.chat_edit_message))
        if (editable) options.add(getString(R.string.chat_delete_message))

        if (options.isEmpty()) return

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.chat_message_actions))
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    getString(R.string.chat_edit_message) -> showEditMessageDialog(message)
                    getString(R.string.chat_delete_message) -> viewModel.deleteMessage(message.messageId)
                }
            }
            .setNegativeButton(R.string.dismiss, null)
            .show()
    }

    private fun showEditMessageDialog(message: ChatMessage) {
        val input = android.widget.EditText(requireContext()).apply {
            setText(message.text)
            setSelection(message.text.length)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.chat_edit_message)
            .setView(input)
            .setPositiveButton(R.string.confirm) { _, _ ->
                viewModel.editMessage(message.messageId, input.text?.toString().orEmpty())
            }
            .setNegativeButton(R.string.dismiss, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        val chatId = chatIdArg.ifBlank {
            val current = viewModel.getCurrentUserId()
            if (current.isBlank() || otherUserId.isBlank()) "" else listOf(current, otherUserId).sorted().joinToString("_")
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

