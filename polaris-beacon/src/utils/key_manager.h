#ifndef KEY_STORAGE_H
#define KEY_STORAGE_H

#include <Preferences.h>
#include <stddef.h>
#include <stdint.h>

#include "protocol/pol_constants.h"

static constexpr const uint8_t HARDCODED_SERVER_X25519_PK[X25519_PK_SIZE] = {
    0x85, 0x20, 0xf0, 0x09, 0x89, 0x30, 0xa7, 0x54, 0x74, 0x8b, 0x7d, 0xdc, 0xb4, 0x3e, 0xf7, 0x5a,
    0x0d, 0xbf, 0x3a, 0x0d, 0x26, 0x38, 0x1a, 0xf4, 0xeb, 0xa4, 0xa9, 0x86, 0xaa, 0x9b, 0x42, 0x20};

class KeyManager {
public:
    KeyManager(const uint8_t (&serverX25519Pk)[X25519_PK_SIZE] = HARDCODED_SERVER_X25519_PK);
    void begin(Preferences& prefs);

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