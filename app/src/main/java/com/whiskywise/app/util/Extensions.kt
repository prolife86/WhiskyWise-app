package com.whiskywise.app.util

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.whiskywise.app.R

fun ImageView.loadWhiskyPhoto(
    context: Context,
    photoPath: String?,
    serverUrl: String,
    token: String,
    skipCache: Boolean = false,
) {
    if (photoPath.isNullOrBlank()) {
        setImageResource(R.drawable.ic_whisky_placeholder)
        return
    }
    val base = serverUrl.trimEnd('/')

    val cleanPath = photoPath.trimStart('/')
        .removePrefix("api/photo/")
        .removePrefix("photos/")

    val url = "$base/api/photo/$cleanPath"

    val glideUrl = GlideUrl(
        url,
        LazyHeaders.Builder()
            .addHeader("Authorization", "Bearer $token")
            .build()
    )

    val request = Glide.with(context)
        .load(glideUrl)
        .placeholder(R.drawable.ic_whisky_placeholder)
        .error(R.drawable.ic_whisky_placeholder)
        .centerCrop()

    // After a server-side rotation the filename is unchanged but the content has
    // changed. skipCache bypasses both memory and disk cache to force a fresh fetch.
    if (skipCache) {
        request
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(this)
    } else {
        request.into(this)
    }
}

fun Double?.formatScore(): String = if (this == null) "—" else String.format("%.1f", this)
fun Double?.formatAbv(): String   = if (this == null) "—" else String.format("%.1f%%", this)
fun Double?.formatPrice(): String = if (this == null) "—" else "€%.2f".format(this)
