#ifndef KEY_STORAGE_H
#define KEY_STORAGE_H

#include <Preferences.h>
#include <stddef.h>
#include <stdint.h>

#include "protocol/pol_constants.h"

/// @brief The hardcoded default public key for the server key exchange (X25519).
static constexpr const uint8_t HARDCODED_SERVER_X25519_PK[X25519_PK_SIZE] = {
    0xdc, 0x8f, 0xbf, 0x40, 0xa9, 0x5e, 0x34, 0x2e, 0xc5, 0xd3, 0x13, 0xc7, 0x13, 0xe6, 0x91, 0x9f,
    0xcc, 0x81, 0x7e, 0x22, 0x07, 0x98, 0xc9, 0x39, 0x20, 0x2c, 0xc8, 0xfb, 0x08, 0x47, 0x8f, 0x7e};

/**
 * @class KeyManager
 * @brief Manages the lifecycle of the beacon cryptographic keys.
 *
 * This class is responsible for loading the beacon Ed25519 (signing) and
 * X25519 (encryption) key pairs from NVS. If the keys are not found or are
 * invalid, it generates new ones and persists them. It also manages the
 * server public key and derives the shared secret (AEAD key) required for
 * encrypted communication.
 */
class KeyManager {
public:
    /**
     * @brief Constructs the KeyManager.
     * @param serverX25519Pk The default server public key to use if one isn't found in NVS.
     */
    KeyManager(const uint8_t (&serverX25519Pk)[X25519_PK_SIZE] = HARDCODED_SERVER_X25519_PK);

    /**
     * @brief Initializes the KeyManager and all cryptographic keys.
     * @param prefs A reference to an initialized Preferences object for NVS access.
     * @return True on success, false if a critical failure occurs.
     */
    bool begin(Preferences& prefs);

    /** @brief Gets the beacon Ed25519 public signing key. */
    const uint8_t* getEd25519Pk() const;

    /** @brief Gets the beacon Ed25519 private signing key. */
    const uint8_t* getEd25519Sk() const;

    /** @brief Gets the beacon X25519 public key for key exchange. */
    const uint8_t* getX25519Pk() const;

    /** @brief Gets the beacon X25519 private key for key exchange. */
    const uint8_t* getX25519Sk() const;

    /** @brief Gets the serverX25519 public key. */
    const uint8_t* getServerX25519Pk() const;

    /** @brief Gets the derived shared secret for AEAD operations. */
    const uint8_t* getAeadKey() const;

private:
    /** @brief Loads or generates and stores the Ed25519 key pair. */
    bool manageEd25519KeyPair(uint8_t pk_out[Ed25519_PK_SIZE], uint8_t sk_out[Ed25519_SK_SIZE]);

    /** @brief Loads or generates and stores the X25519 key pair. */
    bool manageX25519KeyPair(uint8_t pk_out[X25519_PK_SIZE], uint8_t sk_out[X25519_SK_SIZE]);

    /** @brief Loads or uses the hardcoded default for the server public key. */
    bool manageServerX25519PublicKey(uint8_t pk_out[X25519_PK_SIZE],
                                     const uint8_t hardcoded_pk[X25519_PK_SIZE]);

    /** @brief Generates a new Ed25519 key pair. */
    void generateEd25519KeyPair(uint8_t publicKeyOut[Ed25519_PK_SIZE],
                                uint8_t secretKeyOut[Ed25519_SK_SIZE]);

    /** @brief Generates a new Ed25519 key pair. */
    void generateX25519KeyPair(uint8_t publicKeyOut[X25519_PK_SIZE],
                               uint8_t secretKeyOut[X25519_SK_SIZE]);

    /** @brief Derives the shared AEAD key from our private key and the server public key. */
    bool deriveAEADSharedKey(uint8_t sharedKeyOut[SHARED_KEY_SIZE],
                             const uint8_t x25519SecretKey[X25519_SK_SIZE],
                             const uint8_t serverX25519PublicKey[X25519_PK_SIZE]);

    /** @brief Helper to load a key from NVS. */
    bool loadKey(const char* nvs_key_name, uint8_t* key_buffer, size_t expected_len);

    /** @brief Helper to load a key from NVS. */
    bool storeKey(const char* nvs_key_name, const uint8_t* key_buffer, size_t len);

    /** @brief Helper to print a key to the serial monitor for debugging. */
    void printKey(const size_t keyLength, const uint8_t* key) const;

    /// @brief Handle to the NVS storage library.
    Preferences _prefs;

    /// @brief A tag used for logging from this class.
    static constexpr const char* TAG = "[KeyManager]";

    /// @brief The beacon private signing key (Ed25519).
    uint8_t _ed25519Sk[Ed25519_SK_SIZE];

    /// @brief The beacon public signing key (Ed25519).
    uint8_t _ed25519Pk[Ed25519_PK_SIZE];

    /// @brief The beacon private key for Diffie-Hellman key exchange (X25519).
    uint8_t _x25519Sk[X25519_SK_SIZE];

    /// @brief The beacon public key for Diffie-Hellman key exchange (X25519).
    uint8_t _x25519Pk[X25519_PK_SIZE];

    /// @brief The server public key for Diffie-Hellman key exchange (X25519).
    uint8_t _serverX25519Pk[X25519_PK_SIZE];

    /// @brief The derived shared secret key for symmetric encryption (AEAD).
    uint8_t _aeadKey[SHARED_KEY_SIZE];
};

#endif  // KEY_STORAGE_H