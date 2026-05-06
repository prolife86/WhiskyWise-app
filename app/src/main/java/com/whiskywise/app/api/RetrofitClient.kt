package com.whiskywise.app.api

import com.google.gson.GsonBuilder
import com.whiskywise.app.BuildConfig
import com.whiskywise.app.util.TokenStore
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
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        // serializeNulls() ensures fields explicitly set to null in WhiskyRequest are
        // included in the JSON body (as JSON null) rather than being omitted entirely.
        // This is required for the server's partial-update logic to clear a field:
        // the server only clears a field when the key is present with a null value —
        // an absent key means "leave this field unchanged".
        val gson = GsonBuilder().serializeNulls().create()

        _api = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(WhiskyWiseApi::class.java)
    }
}
