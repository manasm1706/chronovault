package com.example.chronovault.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.databinding.ActivityOnboardingBinding
import com.example.chronovault.ui.auth.AuthActivity
import com.example.chronovault.utils.ThemeManager
import com.google.android.material.tabs.TabLayoutMediator

/**
 * OnboardingActivity - ViewPager2 with 3 pages
 * Persists completion with SharedPreferences
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val appearancePrefs = ServiceLocator.providePreferencesManager(this)
        ThemeManager.applyTheme(
            activity = this,
            modeValue = appearancePrefs.getSelectedThemeMode(),
            schemeValue = appearancePrefs.getSelectedColorScheme()
        )
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val pages = listOf(
            OnboardingPage(
                com.example.chronovault.R.string.onboard_title_1,
                com.example.chronovault.R.string.onboard_desc_1,
                com.example.chronovault.R.drawable.splash_gradient
            ),
            OnboardingPage(
                com.example.chronovault.R.string.onboard_title_2,
                com.example.chronovault.R.string.onboard_desc_2,
                com.example.chronovault.R.drawable.splash_gradient
            ),
            OnboardingPage(
                com.example.chronovault.R.string.onboard_title_3,
                com.example.chronovault.R.string.onboard_desc_3,
                com.example.chronovault.R.drawable.splash_gradient
            )
        )

        val adapter = OnboardingAdapter(pages)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.dotsIndicator, binding.viewPager) { _, _ -> }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtons(position, pages.size)
            }
        })

        binding.btnSkip.setOnClickListener { finishOnboarding() }
        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < pages.size - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        updateButtons(0, pages.size)
    }

    private fun updateButtons(position: Int, total: Int) {
        if (position == total - 1) {
            binding.btnNext.text = getString(com.example.chronovault.R.string.button_get_started)
            binding.btnSkip.visibility = View.GONE
        } else {
            binding.btnNext.text = getString(com.example.chronovault.R.string.button_next)
            binding.btnSkip.visibility = View.VISIBLE
        }
    }

    private fun finishOnboarding() {
        val prefs = ServiceLocator.providePreferencesManager(this)
        prefs.setFirstLaunch(false)
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}

data class OnboardingPage(
    val titleRes: Int,
    val descRes: Int,
    val backgroundRes: Int
)



