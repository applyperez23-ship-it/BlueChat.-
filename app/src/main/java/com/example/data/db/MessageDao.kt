package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE is_general_channel = 1 ORDER BY timestamp ASC")
    fun getGeneralChannelMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE message_id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE message_id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: Int)

    @Query("UPDATE messages SET status = :status WHERE chat_id = :chatId AND recipient_id = :currentUserId AND status < :status")
    suspend fun markIncomingMessagesAsRead(chatId: String, currentUserId: String, status: Int = MessageEntity.STATUS_READ)

    @Query("DELETE FROM messages WHERE chat_id = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)
}
