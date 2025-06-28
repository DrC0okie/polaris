
#ifndef POL_CONSTANTS_H
#define POL_CONSTANTS_H

/// @brief The unique, hardcoded ID for this beacon device.
constexpr uint32_t BEACON_ID = 1;

/// @brief The public-facing BLE device name, used in scan responses.
constexpr const char* BLE_DEVICE_NAME = "PoL Beacon";

/// @brief A placeholder manufacturer ID used in BLE advertisements.
constexpr uint16_t MANUFACTURER_ID = 0xFFFF;

/// @brief Size of the random nonce in bytes for PoL requests
constexpr size_t PROTOCOL_NONCE_SIZE = 16;

/// @brief Size of an Ed25519 digital signature in bytes.
constexpr size_t SIG_SIZE = 64;

/// @brief Size of an Ed25519 public key in bytes.
constexpr size_t Ed25519_PK_SIZE = 32;

/// @brief Size of an Ed25519 secret key in bytes.
constexpr size_t Ed25519_SK_SIZE = 64;

/// @brief Size of an X25519 public key in bytes.
constexpr size_t X25519_PK_SIZE = 32;

/// @brief Size of an X25519 private key in bytes.
constexpr size_t X25519_SK_SIZE = 32;

/// @brief Size of the shared secret derived from an X25519 key exchange.
constexpr size_t SHARED_KEY_SIZE = 32;

/// @brief Size of the nonce for ChaCha20-Poly1305 AEAD (IETF standard).
constexpr size_t POL_AEAD_NONCE_SIZE = 12;

/// @brief Size of the authentication tag for ChaCha20-Poly1305 AEAD.
constexpr size_t POL_AEAD_TAG_SIZE = 16;

/// @brief The maximum size of the inner plaintext structure for encrypted messages.
constexpr size_t MAX_INNER_PLAINTEXT_SIZE = 200;

/// @brief The maximum theoretical size of a BLE payload on the characteristic.
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

/// @brief Message type for a request.
constexpr uint8_t MSG_TYPE_REQ = 0x01;

/// @brief Message type for an acknowledgment.
constexpr uint8_t MSG_TYPE_ACK = 0x02;

/// @brief Message type for an error response.
constexpr uint8_t MSG_TYPE_ERR = 0x03;

// --- Hardware Pin Definitions ---
#ifdef PIN_NEOPIXEL
#undef PIN_NEOPIXEL
#endif

/// @brief The GPIO pin connected to the NeoPixel data line.
constexpr int PIN_NEOPIXEL = 39;

/// @brief The number of NeoPixels on the board.
constexpr int NUM_NEOPIXELS = 1;

/**
 * @enum OperationType
 * @brief Defines the set of possible operations in the encrypted message protocol.
 */
enum class OperationType : uint8_t {
    NoOp = 0x00,                 ///< No operation; a ping or keep-alive.
    Reboot = 0x01,               ///< Command to reboot the beacon.
    BlinkLed = 0x02,             ///< Command to start blinking the NeoPixel.
    StopBlink = 0x03,            ///< Command to stop blinking the NeoPixel.
    DisplayText = 0x04,          ///< Command to display text on the OLED screen.
    ClearDisplay = 0x05,         ///< Command to clear the OLED screen.
    RequestBeaconStatus = 0x06,  ///< Command from server requesting beacon status.

    BeaconStatus = 0x80,  ///< A message from beacon containing its status.
    Unknown = 0xFF        ///< Represents an unknown or invalid operation type.
};

#endif  // POL_CONSTANTS_H
