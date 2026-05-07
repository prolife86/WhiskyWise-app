package com.whiskywise.app.util

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.whiskywise.app.R

/**
 * Load a whisky photo from the server into this ImageView.
 *
 * The server rotates photos in-place — the filename never changes after a
 * rotation, so Glide's default cache key (the URL) would serve the stale
 * pre-rotation image indefinitely.
 *
 * Fix: append the whisky's [updatedAt] timestamp as a `?t=` query parameter.
 * When the server rotates a photo it also bumps `updated_at`, so the URL
 * changes → Glide sees a new cache key → fetches the updated image.
 * If [updatedAt] is null (older API responses) the param is omitted and
 * behaviour is identical to before.
 *
 * [skipCache] is kept for the rare case where an immediate force-reload is
 * needed before the next whisky reload has returned a fresh [updatedAt].
 */
fun ImageView.loadWhiskyPhoto(
    context: Context,
    photoPath: String?,
    serverUrl: String,
    token: String,
    skipCache: Boolean = false,
    updatedAt: String? = null,
) {
    if (photoPath.isNullOrBlank()) {
        setImageResource(R.drawable.ic_whisky_placeholder)
        return
    }
    val base = serverUrl.trimEnd('/')

    val cleanPath = photoPath.trimStart('/')
        .removePrefix("api/photo/")
        .removePrefix("photos/")

    // Append updatedAt as a cache-busting query param so Glide's cache key
    // changes whenever the server modifies the photo (e.g. after rotation).
    val cacheBuster = updatedAt?.let { "?t=${it.replace(":", "").replace("-", "").replace("+", "")}" } ?: ""
    val url = "$base/api/photo/$cleanPath$cacheBuster"

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
