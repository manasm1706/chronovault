package com.example.chronovault.ui.capsules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chronovault.databinding.BottomSheetShareCapsuleBinding
import com.example.chronovault.ui.common.UserSearchItem
import com.example.chronovault.ui.common.UserSearchSelectableAdapter
import com.example.chronovault.ui.common.UserSearchViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

class ShareCapsuleBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetShareCapsuleBinding? = null
    private val binding get() = _binding!!
    private lateinit var searchViewModel: UserSearchViewModel
    private lateinit var adapter: UserSearchSelectableAdapter

    private val selected = linkedMapOf<String, UserSearchItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetShareCapsuleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchViewModel = ViewModelProvider(this)[UserSearchViewModel::class.java]
        adapter = UserSearchSelectableAdapter(
            multiSelect = true,
            onUserTapped = { user ->
                if (selected.containsKey(user.id)) {
                    selected.remove(user.id)
                } else {
                    selected[user.id] = user
                }
                renderSelectedChips()
                adapter.notifyDataSetChanged()
            },
            isSelected = { userId -> selected.containsKey(userId) }
        )

        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter

        binding.etUserSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                searchViewModel.searchUsers(s?.toString().orEmpty())
            }
        })

        // Empty query should show friend suggestions immediately.
        searchViewModel.searchUsers("")

        searchViewModel.users.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
            val query = searchViewModel.searchQuery.value.orEmpty()
            binding.tvEmptyUsers.visibility = if (query.isNotBlank() && users.isEmpty()) View.VISIBLE else View.GONE
        }

        searchViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressUsers.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        binding.btnShare.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                bundleOf(RESULT_SELECTED_USER_IDS to selected.keys.toTypedArray())
            )
            dismissAllowingStateLoss()
        }

        renderSelectedChips()
    }

    private fun renderSelectedChips() {
        binding.chipGroupSelectedUsers.removeAllViews()
        selected.values.forEach { user ->
            val chip = Chip(requireContext()).apply {
                text = user.name
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    selected.remove(user.id)
                    renderSelectedChips()
                    adapter.notifyDataSetChanged()
                }
            }
            binding.chipGroupSelectedUsers.addView(chip)
        }
        binding.btnShare.isEnabled = selected.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val RESULT_KEY = "share_capsule_result"
        const val RESULT_SELECTED_USER_IDS = "selected_user_ids"
    }
}



