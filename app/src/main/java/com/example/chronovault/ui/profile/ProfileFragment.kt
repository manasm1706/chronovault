package com.example.chronovault.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.chronovault.MainActivity
import com.example.chronovault.databinding.FragmentProfileBinding
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.ImageConverter
import kotlinx.coroutines.launch

/**
 * ProfileFragment - User profile management
 * Allows editing name, avatar, and logout
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateAvatar(requireContext(), it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.apply {
            ivAvatar.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }

            btnEditName.setOnClickListener {
                val newName = etName.text.toString()
                if (newName.isNotEmpty()) {
                    viewModel.updateName(newName)
                }
            }

            btnLogout.setOnClickListener {
                viewModel.logout()
            }

            btnDeleteAccount.setOnClickListener {
                showDeleteConfirmation()
            }

            etName.isEnabled = false
            etEmail.isEnabled = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userName.observe(viewLifecycleOwner) { name ->
                    binding.etName.setText(name)
                }

                viewModel.userEmail.observe(viewLifecycleOwner) { email ->
                    binding.etEmail.setText(email)
                }

                viewModel.userAvatar.observe(viewLifecycleOwner) { avatar ->
                    avatar?.let { base64 ->
                        val bitmap = ImageConverter.base64ToBitmap(base64)
                        bitmap?.let { binding.ivAvatar.setImageBitmap(it) }
                    }
                }

                viewModel.accountState.observe(viewLifecycleOwner) { state ->
                    handleAccountState(state)
                }

                viewModel.loadingState.observe(viewLifecycleOwner) { state ->
                    when (state) {
                        is LoadingState.Loading -> binding.progressProfile.visibility = View.VISIBLE
                        LoadingState.Success -> binding.progressProfile.visibility = View.GONE
                        is LoadingState.Error -> {
                            binding.progressProfile.visibility = View.GONE
                            showError(state.message)
                        }
                        LoadingState.Idle -> {}
                    }
                }
            }
        }
    }

    private fun handleAccountState(state: AccountState) {
        when (state) {
            is AccountState.Success -> {
                showSuccess(state.message)
                viewModel.resetAccountState()
            }
            AccountState.LogoutSuccess -> {
                (activity as? MainActivity)?.logout()
            }
            AccountState.AccountDeleted -> {
                (activity as? MainActivity)?.logout()
            }
            is AccountState.Error -> {
                showError(state.message)
                viewModel.resetAccountState()
            }
            else -> {}
        }
    }

    private fun showDeleteConfirmation() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

