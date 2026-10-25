package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "call_id")
    val callId: String,

    @ColumnInfo(name = "peer_id")
    val peerId: String,

    @ColumnInfo(name = "peer_name")
    val peerName: String,

    @ColumnInfo(name = "is_video")
    val isVideo: Boolean = false,

    @ColumnInfo(name = "is_incoming")
    val isIncoming: Boolean = false,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long = 0L,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
