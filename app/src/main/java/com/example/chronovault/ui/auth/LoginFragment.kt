package com.example.chronovault.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.chronovault.R
import com.example.chronovault.databinding.FragmentLoginBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * LoginFragment - Email/Password login screen
 * Integrates with Firebase Authentication
 */
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            viewModel.setEmail(binding.etEmail.text.toString().trim())
            viewModel.setPassword(binding.etPassword.text.toString().trim())
            viewModel.login()
        }

        binding.tvSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.observe(viewLifecycleOwner) { state ->
                    handleLoginState(state)
                }

                viewModel.resetPasswordState.observe(viewLifecycleOwner) { state ->
                    handleResetPasswordState(state)
                }
            }
        }
    }

    private fun handleLoginState(state: LoginState) {
        when (state) {
            is LoginState.Idle -> {
                binding.progressLogin.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.tvError.visibility = View.GONE
            }

            is LoginState.Loading -> {
                binding.progressLogin.visibility = View.VISIBLE
                binding.btnLogin.isEnabled = false
                binding.tvError.visibility = View.GONE
            }

            is LoginState.Success -> {
                binding.progressLogin.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                (activity as? AuthActivity)?.navigateToMainApp()
            }

            is LoginState.Error -> {
                binding.progressLogin.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.tvError.text = state.message
                binding.tvError.visibility = View.VISIBLE
                Log.e("APP_ERROR", state.message)
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val emailInput = EditText(requireContext()).apply {
            hint = getString(R.string.label_email)
            setText(binding.etEmail.text?.toString().orEmpty())
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(48, 24, 48, 24)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.auth_forgot_password)
            .setView(emailInput)
            .setPositiveButton(R.string.auth_send_reset_link) { _, _ ->
                viewModel.sendPasswordResetEmail(emailInput.text?.toString().orEmpty())
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun handleResetPasswordState(state: ResetPasswordState) {
        when (state) {
            ResetPasswordState.Idle -> {
                binding.tvForgotPassword.isEnabled = true
            }

            ResetPasswordState.Loading -> {
                binding.progressLogin.visibility = View.VISIBLE
                binding.btnLogin.isEnabled = false
                binding.tvForgotPassword.isEnabled = false
            }

            is ResetPasswordState.Success -> {
                binding.progressLogin.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.tvForgotPassword.isEnabled = true
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                viewModel.resetPasswordResetState()
            }

            is ResetPasswordState.Error -> {
                binding.progressLogin.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.tvForgotPassword.isEnabled = true
                binding.tvError.text = state.message
                binding.tvError.visibility = View.VISIBLE
                Log.e("APP_ERROR", state.message)
                viewModel.resetPasswordResetState()
            }
        }
    }
}

