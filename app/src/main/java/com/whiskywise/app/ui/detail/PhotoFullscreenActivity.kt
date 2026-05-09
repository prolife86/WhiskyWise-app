package com.whiskywise.app.ui.detail

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.whiskywise.app.databinding.ActivityPhotoFullscreenBinding
import com.whiskywise.app.util.loadWhiskyPhoto

class PhotoFullscreenActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PHOTO_PATH = "photo_path"
        const val EXTRA_SERVER_URL = "server_url"
        const val EXTRA_TOKEN      = "token"
        const val EXTRA_UPDATED_AT = "updated_at"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide system bars using the modern WindowInsetsController API.
        // FLAG_FULLSCREEN was deprecated in API 30; WindowInsetsControllerCompat
        // works correctly from API 26 (our minSdk) upwards.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val binding = ActivityPhotoFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path      = intent.getStringExtra(EXTRA_PHOTO_PATH) ?: run { finish(); return }
        val serverUrl = intent.getStringExtra(EXTRA_SERVER_URL) ?: ""
        val token     = intent.getStringExtra(EXTRA_TOKEN)      ?: ""
        val updatedAt = intent.getStringExtra(EXTRA_UPDATED_AT)

        binding.ivFullscreen.loadWhiskyPhoto(
            this, path, serverUrl, token,
            updatedAt = updatedAt,
        )
        binding.root.setOnClickListener { finish() }
    }
}
