package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY last_seen DESC")
    fun getAllDiscoveredUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE is_saved = 1 ORDER BY custom_nickname ASC")
    fun getSavedContacts(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE peer_id = :peerId LIMIT 1")
    suspend fun getUserByPeerId(peerId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET is_saved = :isSaved WHERE peer_id = :peerId")
    suspend fun setContactSavedStatus(peerId: String, isSaved: Boolean)

    @Query("UPDATE users SET custom_nickname = :nickname WHERE peer_id = :peerId")
    suspend fun updateNickname(peerId: String, nickname: String)

    @Query("DELETE FROM users WHERE peer_id = :peerId")
    suspend fun deleteUser(peerId: String)
}
