package com.example.chronovault.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.example.chronovault.R

/**
 * ImageView that renders assets/logo.png in its original colors.
 */
class ThemedLogoImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        LogoImageRenderer.applyTo(this)
    }
}

object LogoImageRenderer {

    fun applyTo(target: AppCompatImageView) {
        runCatching {
            target.context.assets.open("logo.png").use { stream ->
                target.setImageBitmap(BitmapFactory.decodeStream(stream))
            }
        }.onFailure {
            target.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

}



