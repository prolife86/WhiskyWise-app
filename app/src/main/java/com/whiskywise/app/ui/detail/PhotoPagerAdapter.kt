package com.whiskywise.app.ui.detail

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.whiskywise.app.R
import com.whiskywise.app.util.loadWhiskyPhoto

/**
 * Drives the photo ViewPager2 on the detail screen.
 * Only slots with a non-blank photo path are included — empty slots are
 * skipped entirely so the pager never shows a blank page.
 */
class PhotoPagerAdapter(
    private val context: Context,
    private val serverUrl: String,
    private val token: String,
) : RecyclerView.Adapter<PhotoPagerAdapter.VH>() {

    private val photos = mutableListOf<String>()   // non-blank paths only

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
        holder.iv.loadWhiskyPhoto(context, photos[position], serverUrl, token)
    }

    override fun getItemCount() = photos.size
}
