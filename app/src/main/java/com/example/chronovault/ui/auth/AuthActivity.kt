package com.example.chronovault.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.chronovault.MainActivity
import com.example.chronovault.R
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.databinding.ActivityAuthBinding

/**
 * AuthActivity - Container for authentication screens (Login/Signup)
 * Handles navigation between auth screens and to main app
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()

        // Check if user is already logged in
        val authRepository = ServiceLocator.provideAuthRepository(this)
        if (authRepository.isUserLoggedIn()) {
            navigateToMainApp()
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.auth_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }

    /**
     * Called when authentication is successful
     */
    fun navigateToMainApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

