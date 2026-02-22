package com.example.chronovault.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.chronovault.R
import com.example.chronovault.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

/**
 * LoginFragment - Email/Password login screen
 * Integrates with MongoDB Atlas for authentication
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
        binding.etEmail.setOnTextChanged { email ->
            viewModel.setEmail(email)
        }

        binding.etPassword.setOnTextChanged { password ->
            viewModel.setPassword(password)
        }

        binding.btnLogin.setOnClickListener {
            viewModel.login()
        }

        binding.tvSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.observe(viewLifecycleOwner) { state ->
                    handleLoginState(state)
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
                // Navigate to home screen
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }

            is LoginState.Error -> {
                binding.progressLogin.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.tvError.text = state.message
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun <T> com.google.android.material.textfield.TextInputEditText.setOnTextChanged(callback: (String) -> Unit) {
        this.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                callback(this.text.toString())
            }
        }
        // Optional: Real-time input handling
    }
}

