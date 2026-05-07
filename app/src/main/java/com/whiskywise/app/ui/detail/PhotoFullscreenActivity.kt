package com.whiskywise.app.ui.detail

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.whiskywise.app.databinding.ActivityPhotoFullscreenBinding
import com.whiskywise.app.util.TokenStore
import com.whiskywise.app.util.loadWhiskyPhoto

/**
 * Full-screen photo viewer. Black background, fitCenter scaling so the
 * entire image is always visible. Tap anywhere to close.
 *
 * Extras:
 *   EXTRA_PHOTO_PATH  — server photo path (same value passed to loadWhiskyPhoto)
 *   EXTRA_SERVER_URL  — base server URL
 *   EXTRA_TOKEN       — Bearer token
 */
class PhotoFullscreenActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PHOTO_PATH = "photo_path"
        const val EXTRA_SERVER_URL = "server_url"
        const val EXTRA_TOKEN      = "token"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Go truly full-screen: hide status bar and nav bar.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )

        val binding = ActivityPhotoFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path      = intent.getStringExtra(EXTRA_PHOTO_PATH) ?: run { finish(); return }
        val serverUrl = intent.getStringExtra(EXTRA_SERVER_URL) ?: ""
        val token     = intent.getStringExtra(EXTRA_TOKEN)      ?: ""

        binding.ivFullscreen.loadWhiskyPhoto(this, path, serverUrl, token)

        // Tap anywhere to dismiss.
        binding.root.setOnClickListener { finish() }
    }
}
