package com.example.chronovault.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chronovault.databinding.BottomSheetNewChatBinding
import com.example.chronovault.ui.common.UserSearchSelectableAdapter
import com.example.chronovault.ui.common.UserSearchViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NewChatBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNewChatBinding? = null
    private val binding get() = _binding!!
    private val searchViewModel: UserSearchViewModel by viewModels()

    private val adapter by lazy {
        UserSearchSelectableAdapter(
            multiSelect = false,
            onUserTapped = { user ->
                parentFragmentManager.setFragmentResult(
                    RESULT_KEY,
                    bundleOf(RESULT_USER_ID to user.id)
                )
                dismissAllowingStateLoss()
            },
            isSelected = { false }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetNewChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter

        binding.etUserSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                searchViewModel.searchUsers(s?.toString().orEmpty())
            }
        })

        // Empty query should show friends so users can start chat without typing.
        searchViewModel.searchUsers("")

        searchViewModel.users.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
            val query = searchViewModel.searchQuery.value.orEmpty()
            binding.tvEmptyUsers.visibility = if (query.isNotBlank() && users.isEmpty()) View.VISIBLE else View.GONE
        }

        searchViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressUsers.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val RESULT_KEY = "new_chat_result"
        const val RESULT_USER_ID = "result_user_id"
    }
}

