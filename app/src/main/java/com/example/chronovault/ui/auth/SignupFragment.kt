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
import com.example.chronovault.databinding.FragmentSignupBinding
import kotlinx.coroutines.launch

/**
 * SignupFragment - User registration screen
 * Integrates with MongoDB Atlas for authentication
 */
class SignupFragment : Fragment() {

    private lateinit var binding: FragmentSignupBinding
    private val viewModel: SignupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.etName.setOnFocusChangeListener { _, _ ->
            viewModel.setName(binding.etName.text.toString())
        }

        binding.etEmail.setOnFocusChangeListener { _, _ ->
            viewModel.setEmail(binding.etEmail.text.toString())
        }

        binding.etPassword.setOnFocusChangeListener { _, _ ->
            viewModel.setPassword(binding.etPassword.text.toString())
        }

        binding.etConfirmPassword.setOnFocusChangeListener { _, _ ->
            viewModel.setConfirmPassword(binding.etConfirmPassword.text.toString())
        }

        binding.btnSignup.setOnClickListener {
            viewModel.signup()
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.tvLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signupState.observe(viewLifecycleOwner) { state ->
                    handleSignupState(state)
                }
            }
        }
    }

    private fun handleSignupState(state: SignupState) {
        when (state) {
            is SignupState.Idle -> {
                binding.progressSignup.visibility = View.GONE
                binding.btnSignup.isEnabled = true
                binding.tvError.visibility = View.GONE
            }

            is SignupState.Loading -> {
                binding.progressSignup.visibility = View.VISIBLE
                binding.btnSignup.isEnabled = false
                binding.tvError.visibility = View.GONE
            }

            is SignupState.Success -> {
                binding.progressSignup.visibility = View.GONE
                binding.btnSignup.isEnabled = true
                // Navigate to home screen
                findNavController().navigate(R.id.action_signupFragment_to_homeFragment)
            }

            is SignupState.Error -> {
                binding.progressSignup.visibility = View.GONE
                binding.btnSignup.isEnabled = true
                binding.tvError.text = state.message
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }
}

