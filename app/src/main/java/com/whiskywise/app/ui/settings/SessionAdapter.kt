package com.whiskywise.app.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.whiskywise.app.databinding.ItemSessionBinding
import com.whiskywise.app.model.ApiSession

class SessionAdapter(private val onRevoke: (ApiSession) -> Unit) :
    ListAdapter<ApiSession, SessionAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemSessionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ApiSession) {
            val ip  = item.originIp ?: "unknown IP"
            val ver = item.clientVersion?.let { " · v$it" } ?: ""
            val ua  = item.userAgent?.let { "\n$it" } ?: ""
            b.tvSessionMeta.text = "$ip$ver$ua"

            val lastSeen = if (item.lastSeen != null) "Last seen: ${item.lastSeen}" else "Last seen: —"
            b.tvSessionLastSeen.text = "Logged in: ${item.created}  ·  $lastSeen"

            if (item.current) {
                b.tvSessionCurrent.visibility = android.view.View.VISIBLE
                b.btnRevokeSession.isEnabled = false
                b.btnRevokeSession.alpha = 0.35f
            } else {
                b.tvSessionCurrent.visibility = android.view.View.GONE
                b.btnRevokeSession.isEnabled = true
                b.btnRevokeSession.alpha = 1f
                b.btnRevokeSession.setOnClickListener { onRevoke(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ApiSession>() {
            override fun areItemsTheSame(a: ApiSession, b: ApiSession) = a.id == b.id
            override fun areContentsTheSame(a: ApiSession, b: ApiSession) = a == b
        }
    }
}
