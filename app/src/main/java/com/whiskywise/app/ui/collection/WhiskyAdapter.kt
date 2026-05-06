package com.whiskywise.app.ui.collection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.whiskywise.app.databinding.ItemWhiskyBinding
import com.whiskywise.app.model.Whisky
import com.whiskywise.app.util.TokenStore
import com.whiskywise.app.util.formatScore
import com.whiskywise.app.util.loadWhiskyPhoto

class WhiskyAdapter(
    private val onClick: (Whisky) -> Unit,
) : ListAdapter<Whisky, WhiskyAdapter.VH>(DIFF) {

    // Read credentials once per adapter instance rather than on every bind call.
    // EncryptedSharedPreferences construction involves Keystore operations and
    // is too expensive to repeat for every visible list item.
    private var serverUrl: String = ""
    private var token: String = ""

    fun setCredentials(serverUrl: String, token: String) {
        this.serverUrl = serverUrl
        this.token = token
    }

    inner class VH(private val b: ItemWhiskyBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(whisky: Whisky) {
            b.tvName.text        = whisky.name
            b.tvDistillery.text  = whisky.distillery ?: "Unknown distillery"
            b.tvScore.text       = whisky.score.formatScore()
            b.tvStatus.text      = whisky.status?.replaceFirstChar { it.uppercase() } ?: ""
            b.tvRegion.text      = whisky.region ?: ""

            b.ivPhoto.loadWhiskyPhoto(b.root.context, whisky.photoFront, serverUrl, token)

            b.root.setOnClickListener { onClick(whisky) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemWhiskyBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Whisky>() {
            override fun areItemsTheSame(a: Whisky, b: Whisky) = a.id == b.id
            override fun areContentsTheSame(a: Whisky, b: Whisky) = a == b
        }
    }
}
