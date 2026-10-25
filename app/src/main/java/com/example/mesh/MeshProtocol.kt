package com.example.mesh

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object MeshProtocol {
    private const val AES_GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    // Derive a 256-bit AES key deterministically from sender & recipient peer IDs
    fun deriveSharedKey(id1: String, id2: String): SecretKeySpec {
        val sorted = if (id1 < id2) "$id1:$id2:bluechat_salt" else "$id2:$id1:bluechat_salt"
        val sha256 = MessageDigest.getInstance("SHA-256")
        val keyBytes = sha256.digest(sorted.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptPayload(plainText: String, senderId: String, recipientId: String): String {
        return try {
            val key = deriveSharedKey(senderId, recipientId)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(IV_LENGTH)
            // Deterministic or pseudo-random IV based on timestamp and hash
            val hash = MessageDigest.getInstance("MD5").digest(plainText.toByteArray(Charsets.UTF_8))
            System.arraycopy(hash, 0, iv, 0, IV_LENGTH)
            val spec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Prepend IV to ciphertext
            val combined = ByteArray(IV_LENGTH + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH)
            System.arraycopy(encryptedBytes, 0, combined, IV_LENGTH, encryptedBytes.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    fun decryptPayload(cipherTextBase64: String, senderId: String, recipientId: String): String {
        return try {
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            if (combined.size <= IV_LENGTH) return cipherTextBase64
            val iv = ByteArray(IV_LENGTH)
            val encryptedSize = combined.size - IV_LENGTH
            val cipherBytes = ByteArray(encryptedSize)

            System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
            System.arraycopy(combined, IV_LENGTH, cipherBytes, 0, encryptedSize)

            val key = deriveSharedKey(senderId, recipientId)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            cipherTextBase64
        }
    }
}
