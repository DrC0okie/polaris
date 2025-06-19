#ifndef KEY_STORAGE_H
#define KEY_STORAGE_H

#include <Preferences.h>
#include <stddef.h>
#include <stdint.h>

#include "protocol/pol_constants.h"

static constexpr const uint8_t HARDCODED_SERVER_X25519_PK[X25519_PK_SIZE] = {
    0xdc, 0x8f, 0xbf, 0x40, 0xa9, 0x5e, 0x34, 0x2e, 0xc5, 0xd3, 0x13, 0xc7, 0x13, 0xe6, 0x91, 0x9f,
    0xcc, 0x81, 0x7e, 0x22, 0x07, 0x98, 0xc9, 0x39, 0x20, 0x2c, 0xc8, 0xfb, 0x08, 0x47, 0x8f, 0x7e};

class KeyManager {
public:
    KeyManager(const uint8_t (&serverX25519Pk)[X25519_PK_SIZE] = HARDCODED_SERVER_X25519_PK);
    bool begin(Preferences& prefs);

    const uint8_t* getEd25519Pk() const;
    const uint8_t* getEd25519Sk() const;
    const uint8_t* getX25519Pk() const;
    const uint8_t* getX25519Sk() const;
    const uint8_t* getServerX25519Pk() const;
    const uint8_t* getAeadKey() const;

private:
    // Manages Ed25519 key pair
    // Loads SK & PK from NVS. If not found or inconsistent, generates a new pair and stores both.
    // Returns true on success (keys are populated), false on critical NVS failure.
    bool manageEd25519KeyPair(uint8_t pk_out[Ed25519_PK_SIZE], uint8_t sk_out[Ed25519_SK_SIZE]);

    // Manages X25519 key pair
    // Loads SK & PK from NVS. If not found or inconsistent, generates a new pair and stores both.
    // Returns true on success (keys are populated), false on critical NVS failure.
    bool manageX25519KeyPair(uint8_t pk_out[X25519_PK_SIZE], uint8_t sk_out[X25519_SK_SIZE]);

    // Manages Server's X25519 Public Key
    // Loads from NVS. If not found, uses the hardcoded default and stores it.
    // Returns true on success (key is populated), false on critical NVS failure.
    bool manageServerX25519PublicKey(uint8_t pk_out[X25519_PK_SIZE],
                                     const uint8_t hardcoded_pk[X25519_PK_SIZE]);

    // Generate a new Ed25519 key pair
    void generateEd25519KeyPair(uint8_t publicKeyOut[Ed25519_PK_SIZE],
                                uint8_t secretKeyOut[Ed25519_SK_SIZE]);

    // Generate a new X25519 key pair (for encryption/decryption)
    void generateX25519KeyPair(uint8_t publicKeyOut[X25519_PK_SIZE],
                               uint8_t secretKeyOut[X25519_SK_SIZE]);

    // Derive a shared secret using our X25519 secret key and their X25519 public key
    // This raw shared secret will be used as the AEAD key.
    bool deriveAEADSharedKey(uint8_t sharedKeyOut[SHARED_KEY_SIZE],
                             const uint8_t x25519SecretKey[X25519_SK_SIZE],
                             const uint8_t serverX25519PublicKey[X25519_PK_SIZE]);

    // Helper to load a key from NVS
    bool loadKey(const char* nvs_key_name, uint8_t* key_buffer, size_t expected_len);
    // Helper to store a key to NVS
    bool storeKey(const char* nvs_key_name, const uint8_t* key_buffer, size_t len);
    // Helper to print a key
    void printKey(const size_t keyLength, const uint8_t* key) const;

    Preferences _prefs;
    static constexpr const char* TAG = "[KeyManager]";
    uint8_t _ed25519Sk[Ed25519_SK_SIZE];
    uint8_t _ed25519Pk[Ed25519_PK_SIZE];
    uint8_t _x25519Sk[X25519_SK_SIZE];
    uint8_t _x25519Pk[X25519_PK_SIZE];
    uint8_t _serverX25519Pk[X25519_PK_SIZE];
    uint8_t _aeadKey[SHARED_KEY_SIZE];
};

#endif  // KEY_STORAGE_H