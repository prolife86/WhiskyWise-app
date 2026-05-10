package com.whiskywise.app.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.whiskywise.app.databinding.ItemTokenBinding
import com.whiskywise.app.model.TokenListItem

class TokenAdapter(private val onRevoke: (TokenListItem) -> Unit) :
    ListAdapter<TokenListItem, TokenAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemTokenBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: TokenListItem) {
            b.tvTokenName.text = item.name
            val last = if (item.lastUsed != null) "Last used: ${item.lastUsed}" else "Never used"
            val ip  = item.originIp?.let { "  ·  IP: $it" } ?: ""
            val ver = item.clientVersion?.let { "  ·  v$it" } ?: ""
            b.tvTokenMeta.text = "Created: ${item.created}  ·  $last$ip$ver"
            b.btnRevokeToken.setOnClickListener { onRevoke(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTokenBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TokenListItem>() {
            override fun areItemsTheSame(a: TokenListItem, b: TokenListItem) = a.id == b.id
            override fun areContentsTheSame(a: TokenListItem, b: TokenListItem) = a == b
        }
    }
}
