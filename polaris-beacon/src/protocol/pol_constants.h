
#ifndef POL_CONSTANTS_H
#define POL_CONSTANTS_H

// Common constants for the Proof-of-Location protocol
constexpr uint32_t BEACON_ID = 1;

// BLE constants
constexpr const char* BLE_DEVICE_NAME = "PoL Beacon";

// Cryptographic sizes for Ed25519 (Signatures)
constexpr size_t POL_PROTOCOL_NONCE_SIZE = 16;  // Size of the nonce in bytes
constexpr size_t POL_SIG_SIZE = 64;    // Size of the Ed25519 signature in bytes
constexpr size_t POL_Ed25519_PK_SIZE = 32;  // Size of the Ed25519 public key in bytes
constexpr size_t POL_Ed25519_SK_SIZE = 64;

// Cryptographic sizes for X25519 (Key Agreement for AEAD)
constexpr size_t POL_X25519_PK_SIZE = 32;
constexpr size_t POL_X25519_SK_SIZE = 32; // X25519 private key
constexpr size_t POL_SHARED_KEY_SIZE = 32; // Shared secret from X25519

// AEAD specific sizes
constexpr size_t POL_AEAD_NONCE_SIZE = 12; // For IETF ChaCha20-Poly1305
constexpr size_t POL_AEAD_TAG_SIZE = 16;   // For ChaCha20-Poly1305

// Hardcoded Server X25519 Public Key for testing
const uint8_t HARDCODED_SERVER_X25519_PK[POL_X25519_PK_SIZE] = {
    0x85, 0x20, 0xf0, 0x09, 0x89, 0x30, 0xa7, 0x54, 0x74, 0x8b, 0x7d, 0xdc, 0xb4, 0x3e, 0xf7, 0x5a,
    0x0d, 0xbf, 0x3a, 0x0d, 0x26, 0x38, 0x1a, 0xf4, 0xeb, 0xa4, 0xa9, 0x86, 0xaa, 0x9b, 0x42, 0x20};

// NVS constants
constexpr const char* NVS_NAMESPACE = "polaris-beacon";
constexpr const char* NVS_Ed25519_SK_NAME = "beacon_Ed25519_sk";
constexpr const char* NVS_Ed25519_PK_NAME = "beacon_Ed25519_pk";
constexpr const char* NVS_X25519_SK_NAME = "beacon_x25519_sk";
constexpr const char* NVS_X25519_PK_NAME = "beacon_x25519_pk";
constexpr const char* NVS_SERVER_X25519_PK_NAME = "server_x25519_pk";
#endif  // POL_CONSTANTS_H
