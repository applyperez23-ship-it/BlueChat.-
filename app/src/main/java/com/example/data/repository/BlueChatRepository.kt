package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.CallLogEntity
import com.example.data.model.ChatEntity
import com.example.data.model.DeadDropEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

class BlueChatRepository(private val db: AppDatabase) {
    val allDiscoveredUsers: Flow<List<UserEntity>> = db.userDao().getAllDiscoveredUsers()
    val savedContacts: Flow<List<UserEntity>> = db.userDao().getSavedContacts()
    val allChats: Flow<List<ChatEntity>> = db.chatDao().getAllChats()
    val generalChannelMessages: Flow<List<MessageEntity>> = db.messageDao().getGeneralChannelMessages()
    val allCallLogs: Flow<List<CallLogEntity>> = db.callLogDao().getAllCallLogs()

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> {
        return db.messageDao().getMessagesForChat(chatId)
    }

    suspend fun saveOrUpdateUser(user: UserEntity) {
        db.userDao().insertOrUpdateUser(user)
    }

    suspend fun setContactSavedStatus(peerId: String, isSaved: Boolean) {
        db.userDao().setContactSavedStatus(peerId, isSaved)
    }

    suspend fun updateNickname(peerId: String, nickname: String) {
        db.userDao().updateNickname(peerId, nickname)
    }

    suspend fun getUserByPeerId(peerId: String): UserEntity? {
        return db.userDao().getUserByPeerId(peerId)
    }

    suspend fun insertOrUpdateChat(chat: ChatEntity) {
        db.chatDao().insertOrUpdateChat(chat)
    }

    suspend fun resetChatUnread(chatId: String) {
        db.chatDao().resetUnreadCount(chatId)
    }

    suspend fun deleteChat(chatId: String) {
        db.chatDao().deleteChat(chatId)
        db.messageDao().deleteMessagesForChat(chatId)
    }

    suspend fun insertMessage(message: MessageEntity) {
        db.messageDao().insertMessage(message)
    }

    suspend fun updateMessageStatus(messageId: String, status: Int) {
        db.messageDao().updateMessageStatus(messageId, status)
    }

    suspend fun markIncomingRead(chatId: String, myUserId: String) {
        db.messageDao().markIncomingMessagesAsRead(chatId, myUserId, MessageEntity.STATUS_READ)
    }

    suspend fun insertDeadDrop(deadDrop: DeadDropEntity) {
        db.deadDropDao().insertDeadDrop(deadDrop)
    }

    suspend fun getDeadDropsForDestination(destId: String): List<DeadDropEntity> {
        return db.deadDropDao().getDeadDropsForDestination(destId)
    }

    suspend fun removeDeadDrop(packetId: String) {
        db.deadDropDao().deleteDeadDrop(packetId)
    }

    suspend fun insertCallLog(callLog: CallLogEntity) {
        db.callLogDao().insertCallLog(callLog)
    }

    suspend fun deleteCallLog(callId: String) {
        db.callLogDao().deleteCallLog(callId)
    }

    suspend fun clearCallLogs() {
        db.callLogDao().clearCallLogs()
    }
}
