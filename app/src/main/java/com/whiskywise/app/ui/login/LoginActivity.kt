package com.whiskywise.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.whiskywise.app.api.RetrofitClient
import com.whiskywise.app.api.WhiskyWiseRepository
import com.whiskywise.app.databinding.ActivityLoginBinding
import com.whiskywise.app.ui.MainActivity
import com.whiskywise.app.util.TokenStore
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var tokenStore: TokenStore
    private val repo = WhiskyWiseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenStore = TokenStore(this)

        // Auto-login if credentials exist
        if (tokenStore.isLoggedIn()) {
            val url = tokenStore.getServerUrl()!!
            RetrofitClient.init(url, tokenStore)
            goToMain()
            return
        }

        binding.btnLogin.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        val serverUrl = binding.etServerUrl.text.toString().trim()
        val username  = binding.etUsername.text.toString().trim()
        val password  = binding.etPassword.text.toString()

        if (serverUrl.isBlank()) { binding.etServerUrl.error = "Required"; return }
        if (username.isBlank())  { binding.etUsername.error  = "Required"; return }
        if (password.isBlank())  { binding.etPassword.error  = "Required"; return }

        setLoading(true)

        // Initialise Retrofit with a temporary unauthenticated client (no token yet)
        RetrofitClient.init(serverUrl, tokenStore)

        lifecycleScope.launch {
            val result = repo.login(username, password)
            setLoading(false)
            result.fold(
                onSuccess = { tokenData ->
                    tokenStore.saveCredentials(serverUrl, tokenData.token)
                    // Re-init so subsequent calls use the new token
                    RetrofitClient.init(serverUrl, tokenStore)
                    goToMain()
                },
                onFailure = { err ->
                    Snackbar.make(binding.root, err.message ?: "Login failed", Snackbar.LENGTH_LONG).show()
                },
            )
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
