package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dead_drops")
data class DeadDropEntity(
    @PrimaryKey
    @ColumnInfo(name = "packet_id")
    val packetId: String,

    @ColumnInfo(name = "destination_id")
    val destinationId: String,

    @ColumnInfo(name = "ttl")
    val ttl: Int = 10,

    @ColumnInfo(name = "raw_packet_json")
    val rawPacketJson: String,

    @ColumnInfo(name = "stored_at")
    val storedAt: Long = System.currentTimeMillis()
)
