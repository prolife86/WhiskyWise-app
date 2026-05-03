package com.whiskywise.app.util

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.whiskywise.app.R

fun ImageView.loadWhiskyPhoto(
    context: Context,
    photoPath: String?,
    serverUrl: String,
    token: String,
) {
    if (photoPath.isNullOrBlank()) {
        setImageResource(R.drawable.ic_whisky_placeholder)
        return
    }
    val base = if (serverUrl.endsWith("/")) serverUrl.dropLast(1) else serverUrl
    val url = "$base/api/photo/$photoPath"
    val glideUrl = GlideUrl(url, LazyHeaders.Builder()
        .addHeader("Authorization", "Bearer $token")
        .build())
    Glide.with(context)
        .load(glideUrl)
        .placeholder(R.drawable.ic_whisky_placeholder)
        .error(R.drawable.ic_whisky_placeholder)
        .centerCrop()
        .into(this)
}

fun Double?.formatScore(): String = if (this == null) "—" else String.format("%.1f", this)
fun Double?.formatAbv(): String   = if (this == null) "—" else String.format("%.1f%%", this)
fun Double?.formatPrice(): String = if (this == null) "—" else "€%.2f".format(this)
