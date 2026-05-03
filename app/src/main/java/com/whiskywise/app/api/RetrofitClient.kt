package com.whiskywise.app.api

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

    /** Call this once on app start (and again when the server URL changes). */
    fun init(baseUrl: String, tokenStore: TokenStore) {
        // Normalise: ensure trailing slash for Retrofit
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (url == _baseUrl && _api != null) return
        _baseUrl = url

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
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

        _api = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WhiskyWiseApi::class.java)
    }
}
