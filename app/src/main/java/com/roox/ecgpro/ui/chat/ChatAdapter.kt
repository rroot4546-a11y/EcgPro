package com.roox.ecgpro.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.roox.ecgpro.R
import com.roox.ecgpro.data.model.ChatMessage
import java.io.File

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.VH>(DIFF) {
    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_ASSISTANT = 1
        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(a: ChatMessage, b: ChatMessage) = a.id == b.id
            override fun areContentsTheSame(a: ChatMessage, b: ChatMessage) = a == b
        }
    }

    override fun getItemViewType(pos: Int) = if (getItem(pos).role == "user") TYPE_USER else TYPE_ASSISTANT

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvMsg: TextView = v.findViewById(R.id.tvMessage)
        val ivImg: ImageView? = v.findViewById(R.id.ivChatImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): VH {
        val layout = if (vt == TYPE_USER) R.layout.item_chat_user else R.layout.item_chat_assistant
        return VH(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val msg = getItem(pos)
        h.tvMsg.text = msg.content
        h.ivImg?.let { iv ->
            if (msg.imageUri.isNotBlank()) {
                iv.visibility = View.VISIBLE
                iv.load(File(msg.imageUri)) { crossfade(true) }
            } else {
                iv.visibility = View.GONE
            }
        }
    }
}
