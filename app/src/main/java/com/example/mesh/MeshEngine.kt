package com.example.mesh

import android.content.Context
import android.util.Log
import com.example.data.model.ChatEntity
import com.example.data.model.DeadDropEntity
import com.example.data.model.MessageEntity
import com.example.data.model.MeshPacket
import com.example.data.model.UserEntity
import com.example.data.repository.BlueChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class MeshEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: BlueChatRepository,
    val myPeerId: String,
    var myNickname: String
) {
    companion object {
        private const val TAG = "BlueChatMeshEngine"
    }

    private val seenPacketIds = Collections.synchronizedSet(LinkedHashSet<String>())
    private val peerIpMap = ConcurrentHashMap<String, String>()

    // Live discovered peers count for Mesh Health Badge
    private val _activeNearbyPeersCount = MutableStateFlow(0)
    val activeNearbyPeersCount: StateFlow<Int> = _activeNearbyPeersCount.asStateFlow()

    private val bleManager = BleManager(
        context = context,
        scope = scope,
        myPeerId = myPeerId,
        myNickname = myNickname,
        onPeerDiscovered = { peerId, nickname, rssi ->
            handlePeerDiscovered(peerId, nickname, rssi, hops = 1, ip = null)
        }
    )

    private val btClassicServer = BluetoothClassicServer(
        context = context,
        scope = scope,
        onPacketReceived = { rawJson ->
            handleIncomingPacket(rawJson)
        }
    )

    private val wifiLanBridge = WifiLanBridge(
        context = context,
        scope = scope,
        myPeerId = myPeerId,
        onPacketReceived = { rawJson, remoteIp ->
            handleIncomingPacket(rawJson, remoteIp)
        }
    )

    fun start() {
        bleManager.start()
        btClassicServer.startListening()
        wifiLanBridge.start()
        startPeriodicPresenceBroadcast()
    }

    fun stop() {
        bleManager.stop()
        btClassicServer.stopListening()
        wifiLanBridge.stop()
    }

    fun updateNickname(newName: String) {
        myNickname = newName
        bleManager.updateNickname(newName)
        broadcastPresence()
    }

    fun setBatterySaverMode(enabled: Boolean) {
        bleManager.setBatterySaverMode(enabled)
    }

    private fun startPeriodicPresenceBroadcast() {
        scope.launch(Dispatchers.IO) {
            while (true) {
                broadcastPresence()
                kotlinx.coroutines.delay(10000)
            }
        }
    }

    fun broadcastPresence() {
        val ping = MeshPacket(
            senderId = myPeerId,
            senderNickname = myNickname,
            recipientId = MeshPacket.RECIPIENT_ALL,
            type = MeshPacket.TYPE_PING,
            payload = "PING",
            ttl = 1
        )
        dispatchRawPacket(ping.toJson())
    }

    private fun handlePeerDiscovered(peerId: String, nickname: String, rssi: Int, hops: Int, ip: String?) {
        scope.launch(Dispatchers.IO) {
            ip?.let { peerIpMap[peerId] = it }

            val existing = repository.getUserByPeerId(peerId)
            val updatedUser = UserEntity(
                peerId = peerId,
                customNickname = existing?.customNickname ?: nickname,
                statusBio = existing?.statusBio ?: "Conectado a la red BlueChat",
                isSaved = existing?.isSaved ?: false,
                lastSeen = System.currentTimeMillis(),
                routingHops = hops,
                rssi = rssi,
                avatarColorIndex = Math.abs(peerId.hashCode()) % 6,
                ipAddress = ip ?: existing?.ipAddress
            )
            repository.saveOrUpdateUser(updatedUser)

            // Flush buffered dead-drop messages if we have packets stored for this newly visible peer
            flushDeadDropsForPeer(peerId)

            updatePeerCount()
        }
    }

    private suspend fun updatePeerCount() {
        val users = repository.allDiscoveredUsers.firstOrNull() ?: emptyList()
        val now = System.currentTimeMillis()
        // Count peers seen in the last 2 minutes
        val activeCount = users.count { now - it.lastSeen < 120_000 && it.peerId != myPeerId }
        _activeNearbyPeersCount.value = activeCount
    }

    fun sendChatMessage(
        recipientId: String,
        recipientName: String,
        text: String,
        isGeneral: Boolean = false,
        isContactsOnly: Boolean = false,
        isPriority: Boolean = false
    ): MessageEntity {
        val messageId = java.util.UUID.randomUUID().toString()
        val chatId = if (isGeneral) "general_channel" else recipientId
        val ttl = if (isPriority) 20 else 7

        // E2EE encryption for 1-on-1 private messages
        val wirePayload = if (isGeneral) {
            text
        } else {
            MeshProtocol.encryptPayload(text, myPeerId, recipientId)
        }

        val packet = MeshPacket(
            packetId = messageId,
            senderId = myPeerId,
            senderNickname = myNickname,
            recipientId = if (isGeneral) MeshPacket.RECIPIENT_ALL else recipientId,
            type = MeshPacket.TYPE_TEXT,
            payload = wirePayload,
            ttl = ttl,
            isContactsOnly = isContactsOnly,
            isPriority = isPriority
        )

        val messageEntity = MessageEntity(
            messageId = messageId,
            chatId = chatId,
            senderId = myPeerId,
            senderName = myNickname,
            recipientId = recipientId,
            textPayload = text,
            timestamp = System.currentTimeMillis(),
            status = MessageEntity.STATUS_SENT, // Sent out to radio
            isGeneralChannel = isGeneral,
            isContactsOnly = isContactsOnly,
            isPriority = isPriority
        )

        scope.launch(Dispatchers.IO) {
            repository.insertMessage(messageEntity)

            // Update or create chat thread
            val chat = ChatEntity(
                chatId = chatId,
                peerId = recipientId,
                displayName = if (isGeneral) "Canal General" else recipientName,
                unreadCount = 0,
                lastMessageTimestamp = System.currentTimeMillis(),
                lastMessageSnippet = text,
                lastMessageStatus = MessageEntity.STATUS_SENT
            )
            repository.insertOrUpdateChat(chat)

            // Remember our own packet so we don't re-digest it
            seenPacketIds.add(messageId)

            // Route packet
            routeOutboundPacket(packet)
        }

        return messageEntity
    }

    fun sendReadReceipt(chatId: String, senderId: String, messageId: String) {
        val packet = MeshPacket(
            senderId = myPeerId,
            senderNickname = myNickname,
            recipientId = senderId,
            type = MeshPacket.TYPE_ACK_READ,
            payload = messageId,
            ttl = 7
        )
        routeOutboundPacket(packet)
    }

    private fun routeOutboundPacket(packet: MeshPacket) {
        val json = packet.toJson()

        // 1. Direct LAN TCP if we have destination IP
        val targetIp = peerIpMap[packet.recipientId]
        if (targetIp != null && wifiLanBridge.isWifiConnected()) {
            wifiLanBridge.sendTcp(targetIp, json)
        }

        // 2. Wi-Fi Router UDP broadcast
        if (wifiLanBridge.isWifiConnected()) {
            wifiLanBridge.broadcastUdp(json)
        }

        // 3. Bluetooth Classic broadcast & BLE
        btClassicServer.broadcastPacket(json)
    }

    private fun dispatchRawPacket(json: String) {
        if (wifiLanBridge.isWifiConnected()) {
            wifiLanBridge.broadcastUdp(json)
        }
        btClassicServer.broadcastPacket(json)
    }

    private fun handleIncomingPacket(rawJson: String, remoteIp: String? = null) {
        val packet = MeshPacket.fromJson(rawJson) ?: return

        // Duplicate suppression loop check
        if (!seenPacketIds.add(packet.packetId)) {
            return
        }

        // Keep cache bounded
        if (seenPacketIds.size > 2000) {
            val iterator = seenPacketIds.iterator()
            for (i in 0..500) {
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }

        // Update peer discovery
        if (packet.senderId != myPeerId) {
            handlePeerDiscovered(
                peerId = packet.senderId,
                nickname = packet.senderNickname,
                rssi = -65,
                hops = 8 - packet.ttl,
                ip = remoteIp
            )
        }

        scope.launch(Dispatchers.IO) {
            when (packet.type) {
                MeshPacket.TYPE_PING -> {
                    // Handled presence
                }
                MeshPacket.TYPE_TEXT -> {
                    processIncomingTextMessage(packet)
                }
                MeshPacket.TYPE_ACK_DELIVERY -> {
                    val ackedMessageId = packet.payload
                    repository.updateMessageStatus(ackedMessageId, MessageEntity.STATUS_DELIVERED)
                }
                MeshPacket.TYPE_ACK_READ -> {
                    val readMessageId = packet.payload
                    repository.updateMessageStatus(readMessageId, MessageEntity.STATUS_READ)
                }
            }

            // Flood Relay: If not meant exclusively for me and TTL > 1, decrement TTL and forward
            if (packet.recipientId != myPeerId && packet.ttl > 1) {
                val relayPacket = packet.copy(ttl = packet.ttl - 1)
                dispatchRawPacket(relayPacket.toJson())
            }

            // Dead Drop Storage: If this packet is for an offline private peer, store it in Dead Drop table
            if (packet.recipientId != MeshPacket.RECIPIENT_ALL && packet.recipientId != myPeerId) {
                val deadDrop = DeadDropEntity(
                    packetId = packet.packetId,
                    destinationId = packet.recipientId,
                    ttl = packet.ttl - 1,
                    rawPacketJson = packet.toJson()
                )
                repository.insertDeadDrop(deadDrop)
            }
        }
    }

    private suspend fun processIncomingTextMessage(packet: MeshPacket) {
        val isGeneral = packet.recipientId == MeshPacket.RECIPIENT_ALL

        // Check privacy filter for `/contactos`
        if (packet.isContactsOnly && isGeneral) {
            val sender = repository.getUserByPeerId(packet.senderId)
            if (sender == null || !sender.isSaved) {
                // Drop UI rendering if sender is not in saved contacts
                Log.d(TAG, "Dropped /contactos message because sender is not a saved contact")
                return
            }
        }

        // Decrypt 1-on-1 private messages
        val decryptedText = if (isGeneral) {
            packet.payload
        } else {
            MeshProtocol.decryptPayload(packet.payload, packet.senderId, myPeerId)
        }

        val chatId = if (isGeneral) "general_channel" else packet.senderId

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = chatId,
            senderId = packet.senderId,
            senderName = packet.senderNickname,
            recipientId = myPeerId,
            textPayload = decryptedText,
            timestamp = packet.timestamp,
            status = MessageEntity.STATUS_DELIVERED,
            isGeneralChannel = isGeneral,
            isContactsOnly = packet.isContactsOnly,
            isPriority = packet.isPriority
        )
        repository.insertMessage(message)

        // Update chat thread
        val existingChat = repository.allChats.firstOrNull()?.find { it.chatId == chatId }
        val newUnread = (existingChat?.unreadCount ?: 0) + 1
        val chat = ChatEntity(
            chatId = chatId,
            peerId = packet.senderId,
            displayName = if (isGeneral) "Canal General" else packet.senderNickname,
            unreadCount = newUnread,
            lastMessageTimestamp = packet.timestamp,
            lastMessageSnippet = decryptedText,
            lastMessageStatus = MessageEntity.STATUS_DELIVERED
        )
        repository.insertOrUpdateChat(chat)

        // Send back Delivery ACK if private message
        if (!isGeneral && packet.senderId != myPeerId) {
            val ackPacket = MeshPacket(
                senderId = myPeerId,
                senderNickname = myNickname,
                recipientId = packet.senderId,
                type = MeshPacket.TYPE_ACK_DELIVERY,
                payload = packet.packetId,
                ttl = 7
            )
            routeOutboundPacket(ackPacket)
        }
    }

    private suspend fun flushDeadDropsForPeer(peerId: String) {
        val buffered = repository.getDeadDropsForDestination(peerId)
        for (drop in buffered) {
            dispatchRawPacket(drop.rawPacketJson)
            repository.removeDeadDrop(drop.packetId)
        }
    }
}
