package com.whiskywise.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.whiskywise.app.R
import com.whiskywise.app.databinding.CardShareWhiskyBinding
import com.whiskywise.app.model.Whisky
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a [Whisky] (or wishlist item) to a JPEG share card and fires
 * Android's share sheet so the user can send it via WhatsApp, Messages, etc.
 *
 * The card is inflated from [R.layout.card_share_whisky], measured + laid out
 * off-screen at a fixed 1080 px width (≈ 3× a 360 dp card), then drawn to a
 * [Bitmap] via [Canvas].
 *
 * The resulting JPEG is written to [Context.cacheDir]/shares/ and served via
 * [FileProvider] — no WRITE_EXTERNAL_STORAGE permission required.
 *
 * Photos are loaded from the server using the provided [serverUrl] and [token]
 * for authentication.
 */
object WhiskyShareCard {

    private const val CARD_WIDTH_PX = 1080   // 360dp × 3 density
    private const val JPEG_QUALITY  = 92

    /**
     * Build the share card bitmap, write it to cache, and launch the share sheet.
     * Call from a coroutine scope (e.g. lifecycleScope.launch).
     *
     * @param serverUrl  base URL of the WhiskyWise server (e.g. "https://my.server.com")
     * @param token      API bearer token for authenticated photo requests
     * @param isWishlist true → footer reads "MY WISHLIST", status badge hidden
     */
    suspend fun share(
        context: Context,
        whisky: Whisky,
        serverUrl: String,
        token: String,
        isWishlist: Boolean = false,
    ) {
        val uri = withContext(Dispatchers.IO) {
            val bitmap = renderCard(context, whisky, serverUrl, token, isWishlist)
            saveBitmap(context, bitmap, whisky.id)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type  = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${whisky.name}"))
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun renderCard(
        context: Context,
        w: Whisky,
        serverUrl: String,
        token: String,
        isWishlist: Boolean,
    ): Bitmap {
        val inflater = LayoutInflater.from(context)
        val b = CardShareWhiskyBinding.inflate(inflater)

        // ── Photo ────────────────────────────────────────────────────────────
        val photoPath = w.photoFront
        if (!photoPath.isNullOrBlank() && serverUrl.isNotBlank()) {
            b.photoContainer.visibility = View.VISIBLE
            try {
                val photoUrl = "$serverUrl/$photoPath".replace("//", "/")
                    .replace(":/", "://")
                val glideUrl = GlideUrl(
                    photoUrl,
                    LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                )
                val bmp = Glide.with(context)
                    .asBitmap()
                    .load(glideUrl)
                    .submit()
                    .get()   // blocking — we're on Dispatchers.IO
                b.ivPhoto.setImageBitmap(bmp)
            } catch (_: Exception) {
                b.photoContainer.visibility = View.GONE
            }

            // Status badge (collection only)
            if (!isWishlist && !w.status.isNullOrBlank()) {
                b.tvStatusBadge.text = w.status.replaceFirstChar { it.uppercase() }
                b.tvStatusBadge.visibility = View.VISIBLE
            } else {
                b.tvStatusBadge.visibility = View.GONE
            }
        } else {
            b.photoContainer.visibility = View.GONE
        }

        // ── Name + distillery ─────────────────────────────────────────────────
        b.tvName.text = w.name
        val distilleryLine = listOfNotNull(w.distillery, w.region)
            .joinToString(" · ")
            .ifBlank { w.distillery ?: "" }
        b.tvDistillery.text = distilleryLine

        // ── Stats row ─────────────────────────────────────────────────────────
        b.tvAge.text   = w.age ?: "—"
        b.tvAbv.text   = w.abv.formatAbv()
        b.tvPrice.text = w.price.formatPrice()
        b.tvScore.text = w.score.formatScore()

        // ── Tasting notes ─────────────────────────────────────────────────────
        val hasNose   = !w.nose?.trim().isNullOrBlank()
        val hasPalate = !w.palate?.trim().isNullOrBlank()
        val hasFinish = !w.finish?.trim().isNullOrBlank()

        if (hasNose || hasPalate || hasFinish) {
            b.notesContainer.visibility = View.VISIBLE
            b.rowNose.visibility   = if (hasNose)   View.VISIBLE else View.GONE
            b.rowPalate.visibility = if (hasPalate) View.VISIBLE else View.GONE
            b.rowFinish.visibility = if (hasFinish) View.VISIBLE else View.GONE
            if (hasNose)   b.tvNose.text   = w.nose!!.trim()
            if (hasPalate) b.tvPalate.text = w.palate!!.trim()
            if (hasFinish) b.tvFinish.text = w.finish!!.trim()
        }

        // ── Footer ────────────────────────────────────────────────────────────
        b.tvFooterLabel.text = if (isWishlist) "WhiskyWise  /  MY WISHLIST"
                               else            "WhiskyWise  /  MY COLLECTION"
        b.tvRegion.text = (w.region ?: "").uppercase()

        // ── Measure + layout off-screen ───────────────────────────────────────
        val root = b.root
        val widthSpec  = MeasureSpec.makeMeasureSpec(CARD_WIDTH_PX, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        // ── Draw to bitmap ────────────────────────────────────────────────────
        val bitmap = Bitmap.createBitmap(
            root.measuredWidth, root.measuredHeight, Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        root.draw(canvas)
        return bitmap
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap, whiskyId: Int): android.net.Uri {
        val dir  = File(context.cacheDir, "shares").also { it.mkdirs() }
        val file = File(dir, "whisky_${whiskyId}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        bitmap.recycle()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }
}
