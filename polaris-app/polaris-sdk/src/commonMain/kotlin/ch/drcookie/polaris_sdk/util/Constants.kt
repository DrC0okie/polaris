package ch.drcookie.polaris_sdk.util

// Mirroring beacon's pol_constants.h
internal object Constants {
    internal const val ED25519_PK_SIZE = 32
    internal const val ED25519_SK_SIZE = 64 // Libsodium crypto_sign_SECRETKEYBYTES
    internal const val SIG_SIZE = 64
    internal const val PROTOCOL_NONCE_SIZE = 16
}