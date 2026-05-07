package com.whiskywise.app.ui.detail

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
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
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )
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
