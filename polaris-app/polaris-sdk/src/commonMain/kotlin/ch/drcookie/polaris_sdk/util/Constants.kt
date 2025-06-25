package ch.drcookie.polaris_sdk.util

// Mirroring beacon's pol_constants.h
internal object Constants {
    internal const val LEGACY_MANUFACTURER_ID = 0xFFFF // For connectable PoL service adv
    internal const val EXTENDED_MANUFACTURER_ID = 0xABCD // For broadcast-only adv
    internal const val ED25519_PK_SIZE = 32
    internal const val ED25519_SK_SIZE = 64 // Libsodium crypto_sign_SECRETKEYBYTES
    internal const val SIG_SIZE = 64
    internal const val PROTOCOL_NONCE_SIZE = 16

    // Sizes for X25519
    internal  const val X25519_PK_SIZE = 32
    internal  const val X25519_SK_SIZE = 32
    internal  const val SHARED_KEY_SIZE = 32
    internal  const val AEAD_NONCE_SIZE = 12 // For IETF ChaCha20-Poly1305
    internal  const val AEAD_TAG_SIZE = 16
    internal const val POL_SERVICE_UUID = "f44dce36-ffb2-565b-8494-25fa5a7a7cd6"

    // For PoL Token Flow
    internal const val TOKEN_WRITE_UUID = "8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b"
    internal const val TOKEN_INDICATE_UUID = "d234a7d8-ea1f-5299-8221-9cf2f942d3df"

    // For Secure Payload Flow
    internal const val ENCRYPTED_WRITE_UUID = "8ed72380-5adb-4d2d-81fb-ae6610122ee8"
    internal const val ENCRYPTED_INDICATE_UUID = "079b34dd-2310-4b61-89bb-494cc67e097f"
}