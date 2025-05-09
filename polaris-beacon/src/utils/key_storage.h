#ifndef KEY_STORAGE_H
#define KEY_STORAGE_H

#include <Preferences.h>
#include <stddef.h>
#include <stdint.h>

#include "protocol/pol_constants.h"

class KeyStorage {
public:
    KeyStorage(Preferences& prefs);

    // Manages Ed25519 key pair
    // Loads SK & PK from NVS. If not found or inconsistent, generates a new pair and stores both.
    // Returns true on success (keys are populated), false on critical NVS failure.
    bool manageEd25519KeyPair(uint8_t pk_out[POL_Ed25519_PK_SIZE],
                              uint8_t sk_out[POL_Ed25519_SK_SIZE]);

    // Manages X25519 key pair
    // Loads SK & PK from NVS. If not found or inconsistent, generates a new pair and stores both.
    // Returns true on success (keys are populated), false on critical NVS failure.
    bool manageX25519KeyPair(uint8_t pk_out[POL_X25519_PK_SIZE],
                             uint8_t sk_out[POL_X25519_SK_SIZE]);

    // Manages Server's X25519 Public Key
    // Loads from NVS. If not found, uses the hardcoded default and stores it.
    // Returns true on success (key is populated), false on critical NVS failure.
    bool manageServerX25519PublicKey(uint8_t pk_out[POL_X25519_PK_SIZE],
                                     const uint8_t hardcoded_pk[POL_X25519_PK_SIZE]);

private:
    Preferences& _prefs;

    static constexpr const char* TAG = "[KeyStorage]";

    // Helper to load a key from NVS
    bool loadKey(const char* nvs_key_name, uint8_t* key_buffer, size_t expected_len);
    // Helper to store a key to NVS
    bool storeKey(const char* nvs_key_name, const uint8_t* key_buffer, size_t len);
};

#endif  // KEY_STORAGE_H