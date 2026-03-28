package com.example.chronovault.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.chronovault.MainActivity
import com.example.chronovault.R
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.ui.auth.AuthActivity
import com.example.chronovault.ui.onboarding.OnboardingActivity
import com.example.chronovault.utils.ThemeManager

/**
 * SplashActivity - Gradient background, app logo, tagline
 * Auto-navigates after 2 seconds
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val appearancePrefs = ServiceLocator.providePreferencesManager(this)
        ThemeManager.applyTheme(
            activity = this,
            modeValue = appearancePrefs.getSelectedThemeMode(),
            schemeValue = appearancePrefs.getSelectedColorScheme()
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        supportActionBar?.hide()

        Handler(Looper.getMainLooper()).postDelayed({
            navigateNext()
        }, 2000)
    }

    private fun navigateNext() {
        val prefs = ServiceLocator.providePreferencesManager(this)
        val auth = ServiceLocator.provideAuthRepository(this)

        val intent = when {
            prefs.isFirstLaunch() -> Intent(this, OnboardingActivity::class.java)
            auth.isUserLoggedIn() -> Intent(this, MainActivity::class.java)
            else -> Intent(this, AuthActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
