package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DeadDropEntity

@Dao
interface DeadDropDao {
    @Query("SELECT * FROM dead_drops WHERE destination_id = :destinationId")
    suspend fun getDeadDropsForDestination(destinationId: String): List<DeadDropEntity>

    @Query("SELECT * FROM dead_drops ORDER BY stored_at ASC")
    suspend fun getAllDeadDrops(): List<DeadDropEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeadDrop(deadDrop: DeadDropEntity)

    @Query("DELETE FROM dead_drops WHERE packet_id = :packetId")
    suspend fun deleteDeadDrop(packetId: String)

    @Query("DELETE FROM dead_drops WHERE stored_at < :cutoffTime")
    suspend fun purgeExpiredDeadDrops(cutoffTime: Long)
}
