package com.zamouli.aiassistant.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zamouli.aiassistant.R

/**
 * محول للدردشة يستخدم في عرض رسائل المحادثة
 */
class ChatAdapter(private val messages: List<MessageItem>) : 
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    
    // أنواع العناصر
    private val VIEW_TYPE_USER = 1
    private val VIEW_TYPE_ASSISTANT = 2
    
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_ASSISTANT
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutRes = if (viewType == VIEW_TYPE_USER) {
            R.layout.item_message_user
        } else {
            R.layout.item_message
        }
        
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return MessageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    
    override fun getItemCount(): Int = messages.size
    
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        
        fun bind(message: MessageItem) {
            messageText.text = message.text
        }
    }
}