package com.example

import com.example.data.model.MeshPacket
import com.example.mesh.MeshProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
    @Test
    fun meshPacket_serialization_isLossless() {
        val original = MeshPacket(
            senderId = "PEER_ALICE",
            senderNickname = "Alice",
            recipientId = "PEER_BOB",
            type = MeshPacket.TYPE_TEXT,
            payload = "Hola desde BlueChat mesh!",
            ttl = 7,
            isContactsOnly = false,
            isPriority = true
        )

        val json = original.toJson()
        val restored = MeshPacket.fromJson(json)

        assertNotNull(restored)
        assertEquals(original.packetId, restored?.packetId)
        assertEquals(original.senderId, restored?.senderId)
        assertEquals(original.senderNickname, restored?.senderNickname)
        assertEquals(original.recipientId, restored?.recipientId)
        assertEquals(original.type, restored?.type)
        assertEquals(original.payload, restored?.payload)
        assertEquals(original.ttl, restored?.ttl)
        assertEquals(original.isPriority, restored?.isPriority)
    }

    @Test
    fun meshProtocol_encryption_and_decryption() {
        val plainText = "Mensaje confidencial fuera de red"
        val sender = "NODE_A"
        val recipient = "NODE_B"

        val cipher = MeshProtocol.encryptPayload(plainText, sender, recipient)
        val decrypted = MeshProtocol.decryptPayload(cipher, sender, recipient)

        assertEquals(plainText, decrypted)
    }
}
