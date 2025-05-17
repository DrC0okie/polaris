package ch.drcookie.polaris_app

// Mirroring beacon's pol_constants.h
object PoLConstants {
    const val ED25519_PK_SIZE = 32
    const val ED25519_SK_SIZE = 64 // Libsodium crypto_sign_SECRETKEYBYTES
    const val SIG_SIZE = 64
    const val PROTOCOL_NONCE_SIZE = 16

     // Sizes for X25519
     const val X25519_PK_SIZE = 32
     const val X25519_SK_SIZE = 32
     const val SHARED_KEY_SIZE = 32
     const val AEAD_NONCE_SIZE = 12 // For IETF ChaCha20-Poly1305
     const val AEAD_TAG_SIZE = 16
}