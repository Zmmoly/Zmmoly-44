package com.zamouli.aiassistant.ui

/**
 * كائن يمثل رسالة في المحادثة
 */
data class MessageItem(
    val text: String,
    val isUser: Boolean
)