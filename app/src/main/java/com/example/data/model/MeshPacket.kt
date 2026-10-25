package com.example.data.model

import org.json.JSONObject
import java.util.UUID

data class MeshPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderNickname: String,
    val recipientId: String, // "ALL" for public broadcast, or specific peerId
    val type: Int,           // 1=TEXT, 2=ACK_DELIVERY, 3=ACK_READ, 4=PING, 5=FILE_HEADER, 6=CALL_SIGNAL
    val payload: String,
    val ttl: Int = 7,        // Max 20 for @todos
    val isContactsOnly: Boolean = false,
    val isPriority: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_TEXT = 1
        const val TYPE_ACK_DELIVERY = 2
        const val TYPE_ACK_READ = 3
        const val TYPE_PING = 4
        const val TYPE_FILE_HEADER = 5
        const val TYPE_CALL_SIGNAL = 6

        const val RECIPIENT_ALL = "ALL"

        fun fromJson(jsonStr: String): MeshPacket? {
            return try {
                val json = JSONObject(jsonStr)
                MeshPacket(
                    packetId = json.getString("id"),
                    senderId = json.getString("sid"),
                    senderNickname = json.getString("sname"),
                    recipientId = json.getString("rid"),
                    type = json.getInt("t"),
                    payload = json.getString("p"),
                    ttl = json.optInt("ttl", 7),
                    isContactsOnly = json.optBoolean("co", false),
                    isPriority = json.optBoolean("pr", false),
                    timestamp = json.optLong("ts", System.currentTimeMillis())
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("id", packetId)
        json.put("sid", senderId)
        json.put("sname", senderNickname)
        json.put("rid", recipientId)
        json.put("t", type)
        json.put("p", payload)
        json.put("ttl", ttl)
        json.put("co", isContactsOnly)
        json.put("pr", isPriority)
        json.put("ts", timestamp)
        return json.toString()
    }
}
