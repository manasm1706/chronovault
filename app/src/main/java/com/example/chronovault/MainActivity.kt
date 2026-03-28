package com.example.chronovault

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.chronovault.databinding.ActivityMainBinding
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.ui.chat.ChatFragment
import com.example.chronovault.ui.auth.AuthActivity
import com.example.chronovault.services.ForegroundLocationService
import com.example.chronovault.utils.NotificationHelper
import com.example.chronovault.utils.ThemeManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startLocationTrackingIfPermitted()
        } else {
            handleLocationPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val appearancePrefs = ServiceLocator.providePreferencesManager(this)
        ThemeManager.applyTheme(
            activity = this,
            modeValue = appearancePrefs.getSelectedThemeMode(),
            schemeValue = appearancePrefs.getSelectedColorScheme()
        )
        super.onCreate(savedInstanceState)

        // Check if user is logged in
        val authRepository = ServiceLocator.provideAuthRepository(this)
        if (!authRepository.isUserLoggedIn()) {
            // Redirect to AuthActivity
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_map,
                R.id.navigation_capsules,
                R.id.chatListFragment,
                R.id.navigation_profile
            )
        )
        @Suppress("UNUSED_VARIABLE")
        val _keepTopLevelConfig = appBarConfiguration

        navView.setupWithNavController(navController)
        ensureLocationPermissionFlow()

        navView.menu.findItem(R.id.navigation_home)?.isChecked = true

        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d("NAV", destination.id.toString())
            val selectedTabId = when (destination.id) {
                R.id.chatFragment,
                R.id.chatListFragment -> R.id.chatListFragment
                R.id.navigation_home,
                R.id.navigation_map,
                R.id.navigation_capsules,
                R.id.navigation_profile -> destination.id
                else -> null
            }

            selectedTabId?.let { tabId ->
                if (navView.selectedItemId != tabId) {
                    navView.menu.findItem(tabId)?.isChecked = true
                }
                animateBottomNavSelection(navView, tabId)
            }
        }

        navView.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.navigation_home) {
                navController.popBackStack(R.id.navigation_home, false)
            } else {
                navigateToTopLevelDestination(navController, item.itemId)
            }
            animateBottomNavSelection(navView, item.itemId)
        }

        navView.setOnItemSelectedListener { item ->
            val handled = if (item.itemId == R.id.navigation_home) {
                navController.popBackStack(R.id.navigation_home, false) ||
                    navigateToTopLevelDestination(navController, R.id.navigation_home)
            } else {
                navigateToTopLevelDestination(navController, item.itemId)
            }
            if (handled) animateBottomNavSelection(navView, item.itemId)
            handled
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentDestinationId = navController.currentDestination?.id
                if (currentDestinationId == R.id.navigation_home) {
                    finish()
                    return
                }

                val popped = navController.popBackStack()
                if (!popped) {
                    finish()
                }
            }
        })

        handleChatNotificationNavigation(intent, navController)
    }

    private fun navigateToTopLevelDestination(
        navController: androidx.navigation.NavController,
        destinationId: Int
    ): Boolean {
        if (navController.currentDestination?.id == destinationId) return true

        val navOptions = navOptions {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
        }

        return runCatching {
            navController.navigate(destinationId, null, navOptions)
            true
        }.getOrDefault(false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment ?: return
        handleChatNotificationNavigation(intent, navHostFragment.navController)
    }

    private fun handleChatNotificationNavigation(intent: Intent?, navController: androidx.navigation.NavController) {
        val chatId = intent?.getStringExtra(NotificationHelper.EXTRA_NAV_CHAT_ID).orEmpty()
        val otherUserId = intent?.getStringExtra(NotificationHelper.EXTRA_NAV_CHAT_USER_ID).orEmpty()
        if (chatId.isBlank() || otherUserId.isBlank()) return

        runCatching {
            navController.navigate(
                R.id.chatFragment,
                Bundle().apply {
                    putString(ChatFragment.ARG_CHAT_ID, chatId)
                    putString(ChatFragment.ARG_OTHER_USER_ID, otherUserId)
                }
            )
        }

        intent?.removeExtra(NotificationHelper.EXTRA_NAV_CHAT_ID)
        intent?.removeExtra(NotificationHelper.EXTRA_NAV_CHAT_USER_ID)
    }

    private fun ensureLocationPermissionFlow() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationTrackingIfPermitted()
            return
        }

        val prefs = getSharedPreferences("permissions", MODE_PRIVATE)
        val askedBefore = prefs.getBoolean(KEY_ASKED_LOCATION_PERMISSION, false)
        if (!askedBefore) {
            prefs.edit().putBoolean(KEY_ASKED_LOCATION_PERMISSION, true).apply()
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        if (shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            showLocationRationaleDialog()
        } else {
            showLocationPermanentlyDeniedDialog()
        }
    }

    private fun showLocationRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.location_permission_title)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(R.string.confirm) { _, _ ->
                locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setNegativeButton(R.string.dismiss, null)
            .show()
    }

    private fun showLocationPermanentlyDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.location_permission_title)
            .setMessage(R.string.location_permission_settings_message)
            .setPositiveButton(R.string.button_open_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.dismiss, null)
            .show()
    }

    private fun handleLocationPermissionDenied() {
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            showLocationRationaleDialog()
        } else {
            showLocationPermanentlyDeniedDialog()
        }
    }

    private fun startLocationTrackingIfPermitted() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val serviceIntent = Intent(this, ForegroundLocationService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun animateBottomNavSelection(navView: BottomNavigationView, itemId: Int) {
        val selectedItemView = navView.findViewById<View>(itemId) ?: return
        selectedItemView.animate()
            .scaleX(1.07f)
            .scaleY(1.07f)
            .setDuration(120)
            .withEndAction {
                selectedItemView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    /**
     * Called from ProfileFragment when user logs out
     */
    fun logout() {
        val authRepository = ServiceLocator.provideAuthRepository(this)
        authRepository.logoutUser()

        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val KEY_ASKED_LOCATION_PERMISSION = "asked_location_permission"
    }
}

