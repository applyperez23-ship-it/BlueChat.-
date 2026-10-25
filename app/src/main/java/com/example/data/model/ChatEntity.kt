package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    @ColumnInfo(name = "chat_id")
    val chatId: String, // peerId or "general_channel"

    @ColumnInfo(name = "peer_id")
    val peerId: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "unread_count")
    val unreadCount: Int = 0,

    @ColumnInfo(name = "last_message_timestamp")
    val lastMessageTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_message_snippet")
    val lastMessageSnippet: String = "",

    @ColumnInfo(name = "last_message_status")
    val lastMessageStatus: Int = 0 // 0=clock, 1=single, 2=double gray, 3=double blue
)
