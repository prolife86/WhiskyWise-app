package com.whiskywise.app.ui.detail

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.whiskywise.app.R
import com.whiskywise.app.util.loadWhiskyPhoto

/**
 * Drives the photo ViewPager2 on the detail screen.
 * Only slots with a non-blank photo path are included.
 * Tapping a photo opens [PhotoFullscreenActivity].
 */
class PhotoPagerAdapter(
    private val context: Context,
    private val serverUrl: String,
    private val token: String,
) : RecyclerView.Adapter<PhotoPagerAdapter.VH>() {

    private val photos = mutableListOf<String>()

    fun submitPhotos(front: String?, back: String?, cask: String?) {
        photos.clear()
        listOfNotNull(
            front?.takeIf { it.isNotBlank() },
            back?.takeIf  { it.isNotBlank() },
            cask?.takeIf  { it.isNotBlank() },
        ).forEach { photos.add(it) }
        notifyDataSetChanged()
    }

    fun count() = photos.size

    inner class VH(val iv: ImageView) : RecyclerView.ViewHolder(iv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val iv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_page, parent, false) as ImageView
        return VH(iv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val path = photos[position]
        holder.iv.loadWhiskyPhoto(context, path, serverUrl, token)
        holder.iv.setOnClickListener {
            context.startActivity(
                Intent(context, PhotoFullscreenActivity::class.java).apply {
                    putExtra(PhotoFullscreenActivity.EXTRA_PHOTO_PATH, path)
                    putExtra(PhotoFullscreenActivity.EXTRA_SERVER_URL, serverUrl)
                    putExtra(PhotoFullscreenActivity.EXTRA_TOKEN, token)
                }
            )
        }
    }

    override fun getItemCount() = photos.size
}
