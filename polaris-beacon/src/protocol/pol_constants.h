
#ifndef POL_CONSTANTS_H
#define POL_CONSTANTS_H

// Common constants for the Proof-of-Location protocol
constexpr uint32_t BEACON_ID = 1;

// BLE constants
constexpr const char* BLE_DEVICE_NAME = "PoL Beacon";

// Cryptographic sizes for Ed25519 (Signatures)
constexpr size_t PROTOCOL_NONCE_SIZE = 16;  // Size of the nonce in bytes
constexpr size_t SIG_SIZE = 64;             // Size of the Ed25519 signature in bytes
constexpr size_t Ed25519_PK_SIZE = 32;      // Size of the Ed25519 public key in bytes
constexpr size_t Ed25519_SK_SIZE = 64;

// Cryptographic sizes for X25519 (Key Agreement for AEAD)
constexpr size_t X25519_PK_SIZE = 32;
constexpr size_t X25519_SK_SIZE = 32;   // X25519 private key
constexpr size_t SHARED_KEY_SIZE = 32;  // Shared secret from X25519

// AEAD specific sizes
constexpr size_t POL_AEAD_NONCE_SIZE = 12;        // For IETF ChaCha20-Poly1305
constexpr size_t POL_AEAD_TAG_SIZE = 16;          // For ChaCha20-Poly1305
constexpr size_t MAX_INNER_PLAINTEXT_SIZE = 200;  // Max size for the inner plaintext
constexpr size_t MAX_BLE_PAYLOAD_SIZE = 244;

// NVS constants
// WARNING: do not exceed 15 characters for NVS key names!
constexpr const char* NVS_NAMESPACE = "polaris-beacon";
constexpr const char* NVS_Ed25519_SK_NAME = "bcn_Ed25519_sk";
constexpr const char* NVS_Ed25519_PK_NAME = "bcn_Ed25519_pk";
constexpr const char* NVS_X25519_SK_NAME = "bcn_x25519_sk";
constexpr const char* NVS_X25519_PK_NAME = "bcn_x25519_pk";
constexpr const char* NVS_SERVER_X25519_PK_NAME = "srv_x25519_pk";
constexpr const char* NVS_ENC_MSG_ID_COUNTER = "enc_msg_id_ctr";

// Encrypted message protocol constants
constexpr uint8_t MSG_TYPE_REQ = 0x01;
constexpr uint8_t MSG_TYPE_ACK = 0x02;
constexpr uint8_t MSG_TYPE_ERR = 0x03;

#endif  // POL_CONSTANTS_H
