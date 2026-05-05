package com.whiskywise.app

import android.app.Application
import com.whiskywise.app.api.RetrofitClient
import com.whiskywise.app.util.TokenStore

class WhiskyWiseApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialise RetrofitClient as early as possible so it is ready
        // regardless of which Activity Android restores first.
        // If no credentials are stored yet this is a no-op — LoginActivity
        // will call init() again after the user logs in.
        val store = TokenStore(this)
        val url   = store.getServerUrl()
        if (url != null) {
            RetrofitClient.init(url, store)
        }
    }
}
