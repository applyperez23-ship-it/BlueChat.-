package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String,

    @ColumnInfo(name = "chat_id")
    val chatId: String,

    @ColumnInfo(name = "sender_id")
    val senderId: String,

    @ColumnInfo(name = "sender_name")
    val senderName: String,

    @ColumnInfo(name = "recipient_id")
    val recipientId: String,

    @ColumnInfo(name = "text_payload")
    val textPayload: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "status")
    val status: Int = 0, // 0=Clock, 1=Sent, 2=Delivered, 3=Read

    @ColumnInfo(name = "is_general_channel")
    val isGeneralChannel: Boolean = false,

    @ColumnInfo(name = "is_contacts_only")
    val isContactsOnly: Boolean = false,

    @ColumnInfo(name = "is_priority")
    val isPriority: Boolean = false,

    @ColumnInfo(name = "file_name")
    val fileName: String? = null,

    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0L,

    @ColumnInfo(name = "file_path")
    val filePath: String? = null,

    @ColumnInfo(name = "waiting_wifi")
    val waitingWifi: Boolean = false
) {
    companion object {
        const val STATUS_CLOCK = 0
        const val STATUS_SENT = 1
        const val STATUS_DELIVERED = 2
        const val STATUS_READ = 3
    }
}
