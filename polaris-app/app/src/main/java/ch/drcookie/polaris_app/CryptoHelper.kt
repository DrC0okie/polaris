package ch.drcookie.polaris_app

import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.PublicKeySign
import com.google.crypto.tink.PublicKeyVerify
import com.google.crypto.tink.signature.SignatureConfig
import com.google.crypto.tink.signature.SignatureKeyTemplates
import java.security.GeneralSecurityException

object CryptoHelper {
    init {
        SignatureConfig.register()
    }

    data class KeyPair(
        val privateHandle: KeysetHandle,
        val publicHandle: KeysetHandle
    )

    // Generate a new Ed25519 key pair
    fun generateKeyPair(): KeyPair {
        val privateHandle = KeysetHandle.generateNew(SignatureKeyTemplates.ED25519)
        val publicHandle = privateHandle.publicKeysetHandle
        return KeyPair(privateHandle, publicHandle)
    }

    // Sign a message using the private key
    fun sign(message: ByteArray, privateHandle: KeysetHandle): ByteArray {
        val signer = privateHandle.getPrimitive(PublicKeySign::class.java)
        return signer.sign(message)
    }

    // Verify a signature using the public key
    fun verify(message: ByteArray, signature: ByteArray, publicHandle: KeysetHandle): Boolean {
        return try {
            val verifier = publicHandle.getPrimitive(PublicKeyVerify::class.java)
            verifier.verify(signature, message)
            true
        } catch (e: GeneralSecurityException) {
            false
        }
    }

    fun extractPublicKeyBytes(publicHandle: KeysetHandle): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        val writer = com.google.crypto.tink.JsonKeysetWriter.withOutputStream(stream)
        publicHandle.writeNoSecret(writer)
        val json = stream.toString("UTF-8")

        // Parse JSON to extract raw public key bytes
        val regex = Regex("\"keyValue\":\\s*\"([^\"]+)\"")
        val match = regex.find(json) ?: error("Public key not found")
        val base64 = match.groupValues[1]
        return android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
    }
}