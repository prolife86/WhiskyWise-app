package com.whiskywise.app.api

import com.google.gson.GsonBuilder
import com.whiskywise.app.BuildConfig
import com.whiskywise.app.util.TokenStore
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var _api: WhiskyWiseApi? = null
    private var _baseUrl: String = ""

    val api: WhiskyWiseApi
        get() = _api ?: error("RetrofitClient not initialised — call init() first")

    /**
     * Call this once on app start (and again when the server URL changes).
     *
     * Note: the Bearer token is read dynamically from [TokenStore] on every request
     * inside the auth interceptor, so a URL-identical re-init is intentionally skipped.
     * If you ever change the token storage mechanism, revisit this guard.
     */
    fun init(baseUrl: String, tokenStore: TokenStore) {
        // Normalise: ensure trailing slash for Retrofit
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (url == _baseUrl && _api != null) return
        _baseUrl = url

        // Only log request/response bodies in debug builds — release builds must never
        // log Bearer tokens or whisky data to logcat.
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = tokenStore.getToken()
                val builder = chain.request().newBuilder()
                    // Always send the app version so the server can track which
                    // client version is associated with each API token.
                    .addHeader("X-Client-Version", BuildConfig.VERSION_NAME)
                if (token != null) {
                    builder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Retry once automatically on connection failure (e.g. mid-request
            // network switch from WiFi to 5G). OkHttp only retries idempotent
            // requests (GET, HEAD) by default, so POST/PUT are safe.
            .retryOnConnectionFailure(true)
            // Keep connections alive for at most 30 s with a pool of 5.
            // A short keepalive prevents stale sockets from surviving a network
            // switch, which is the root cause of the WiFi → 5G hang.
            .connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
            .build()

        // serializeNulls() ensures fields explicitly set to null in WhiskyRequest
        // are included in the JSON body so the server can clear them.
        val gson = GsonBuilder().serializeNulls().create()

        _api = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(WhiskyWiseApi::class.java)
    }
}
