package com.example.chronovault.ui.profile

import android.net.Uri
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chronovault.MainActivity
import com.example.chronovault.R
import com.example.chronovault.databinding.FragmentProfileBinding
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.ImageConverter
import com.example.chronovault.utils.ThemeManager
import kotlinx.coroutines.launch

/**
 * ProfileFragment - User profile management
 * Allows editing name, avatar, and logout
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var friendRequestsAdapter: FriendRequestsAdapter
    private var isInitializingAppearanceControls: Boolean = false

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
        setupFriendsList()
        setupUI()
        observeViewModel()
    }

    private fun setupFriendsList() {
        friendsAdapter = FriendsAdapter { friend ->
            viewModel.acceptFriend(friend.friendUserId)
        }
        friendRequestsAdapter = FriendRequestsAdapter(
            onAccept = { request -> viewModel.acceptFriendRequest(request) },
            onReject = { request -> viewModel.rejectFriendRequest(request) }
        )
        binding.rvFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFriends.adapter = friendsAdapter
        binding.rvFriendRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFriendRequests.adapter = friendRequestsAdapter
    }

    private fun setupUI() {
        binding.apply {
            ivAvatar.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }

            btnEditName.setOnClickListener {
                val newName = etNameEdit.text?.toString().orEmpty()
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

            listOf(
                btnEditName,
                btnCopyUserId,
                btnAddFriend,
                btnLogout,
                btnDeleteAccount
            ).forEach { button ->
                button.setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80L).start()
                        android.view.MotionEvent.ACTION_UP -> {
                            v.performClick()
                            v.animate().scaleX(1f).scaleY(1f).setDuration(80L).start()
                        }
                        android.view.MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(80L).start()
                    }
                    false
                }
            }

            btnCopyUserId.setOnClickListener {
                copyUserIdToClipboard(binding.tvUserIdValue.text?.toString().orEmpty())
            }

            btnAddFriend.setOnClickListener {
                val friendId = etFriendId.text?.toString().orEmpty()
                if (friendId.isNotBlank()) {
                    viewModel.sendFriendRequest(friendId)
                }
            }

            switchNotificationSound.setOnCheckedChangeListener { _, isChecked ->
                if (!isInitializingAppearanceControls) {
                    viewModel.setNotificationSoundEnabled(isChecked)
                }
            }

            switchNotificationVibration.setOnCheckedChangeListener { _, isChecked ->
                if (!isInitializingAppearanceControls) {
                    viewModel.setNotificationVibrationEnabled(isChecked)
                }
            }

            chipGroupThemeMode.setOnCheckedStateChangeListener { _, checkedIds ->
                if (isInitializingAppearanceControls || checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
                val selectedMode = when (checkedIds.first()) {
                    R.id.chip_theme_light -> ThemeManager.ThemeMode.LIGHT.prefValue
                    R.id.chip_theme_dark -> ThemeManager.ThemeMode.DARK.prefValue
                    else -> ThemeManager.ThemeMode.SYSTEM.prefValue
                }
                if (selectedMode != viewModel.getSelectedThemeMode()) {
                    viewModel.setSelectedThemeMode(selectedMode)
                    applyAppearanceAndRecreate()
                }
            }

            chipGroupColorScheme.setOnCheckedStateChangeListener { _, checkedIds ->
                if (isInitializingAppearanceControls || checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
                val selectedScheme = when (checkedIds.first()) {
                    R.id.chip_scheme_blue -> ThemeManager.ColorScheme.DEEP_BLUE.prefValue
                    R.id.chip_scheme_ochre -> ThemeManager.ColorScheme.WARM_OCHRE.prefValue
                    R.id.chip_scheme_gray -> ThemeManager.ColorScheme.NEUTRAL_GRAY.prefValue
                    else -> ThemeManager.ColorScheme.METALLIC_GREEN.prefValue
                }
                if (selectedScheme != viewModel.getSelectedColorScheme()) {
                    viewModel.setSelectedColorScheme(selectedScheme)
                    applyAppearanceAndRecreate()
                }
            }

            bindSettingsControls()
        }
    }

    private fun bindSettingsControls() {
        isInitializingAppearanceControls = true

        binding.switchNotificationSound.isChecked = viewModel.getNotificationSoundEnabled()
        binding.switchNotificationVibration.isChecked = viewModel.getNotificationVibrationEnabled()

        when (viewModel.getSelectedThemeMode()) {
            ThemeManager.ThemeMode.LIGHT.prefValue -> binding.chipThemeLight.isChecked = true
            ThemeManager.ThemeMode.DARK.prefValue -> binding.chipThemeDark.isChecked = true
            else -> binding.chipThemeSystem.isChecked = true
        }

        when (viewModel.getSelectedColorScheme()) {
            ThemeManager.ColorScheme.DEEP_BLUE.prefValue -> binding.chipSchemeBlue.isChecked = true
            ThemeManager.ColorScheme.WARM_OCHRE.prefValue -> binding.chipSchemeOchre.isChecked = true
            ThemeManager.ColorScheme.NEUTRAL_GRAY.prefValue -> binding.chipSchemeGray.isChecked = true
            else -> binding.chipSchemeGreen.isChecked = true
        }

        isInitializingAppearanceControls = false
    }

    private fun applyAppearanceAndRecreate() {
        val hostActivity = activity ?: return
        ThemeManager.applyTheme(
            activity = hostActivity,
            modeValue = viewModel.getSelectedThemeMode(),
            schemeValue = viewModel.getSelectedColorScheme()
        )
        hostActivity.recreate()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userName.observe(viewLifecycleOwner) { name ->
                    binding.etName.setText(name)
                    if (binding.etNameEdit.text.isNullOrBlank()) {
                        binding.etNameEdit.setText(name)
                    }
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

                viewModel.userId.observe(viewLifecycleOwner) { id ->
                    binding.tvUserIdValue.text = id
                }

                viewModel.friends.observe(viewLifecycleOwner) { friendList ->
                    friendsAdapter.submitList(friendList)
                    binding.tvNoFriends.visibility = if (friendList.isEmpty()) View.VISIBLE else View.GONE
                }

                viewModel.friendRequests.observe(viewLifecycleOwner) { requests ->
                    friendRequestsAdapter.submitList(requests)
                    binding.tvNoFriendRequests.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
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
            .setTitle(com.example.chronovault.R.string.profile_delete_account_title)
            .setMessage(com.example.chronovault.R.string.profile_delete_account_message)
            .setPositiveButton(com.example.chronovault.R.string.button_delete) { _, _ ->
                viewModel.deleteAccount()
            }
            .setNegativeButton(com.example.chronovault.R.string.button_cancel, null)
            .show()
    }

    private fun copyUserIdToClipboard(userId: String) {
        if (userId.isBlank()) return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(com.example.chronovault.R.string.profile_user_id_clip_label), userId))
        showSuccess(getString(com.example.chronovault.R.string.profile_user_id_copied))
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

