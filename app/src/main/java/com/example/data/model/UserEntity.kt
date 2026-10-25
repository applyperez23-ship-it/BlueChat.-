package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "peer_id")
    val peerId: String,

    @ColumnInfo(name = "custom_nickname")
    val customNickname: String,

    @ColumnInfo(name = "status_bio")
    val statusBio: String = "Conectado por BlueChat",

    @ColumnInfo(name = "is_saved")
    val isSaved: Boolean = false,

    @ColumnInfo(name = "last_seen")
    val lastSeen: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "routing_hops")
    val routingHops: Int = 1,

    @ColumnInfo(name = "rssi")
    val rssi: Int = -60,

    @ColumnInfo(name = "avatar_color_index")
    val avatarColorIndex: Int = 0,

    @ColumnInfo(name = "ip_address")
    val ipAddress: String? = null
)
