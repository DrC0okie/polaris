#include "key_storage.h"

#include <HardwareSerial.h>
#include <string.h>

#include "protocol/crypto.h"
#include "protocol/pol_constants.h"

KeyStorage::KeyStorage(Preferences& prefs) : _prefs(prefs) {
}

bool KeyStorage::loadKey(const char* nvs_key_name, uint8_t* key_buffer, size_t expected_len) {
    size_t len_in_nvs = _prefs.getBytesLength(nvs_key_name);
    if (len_in_nvs == expected_len) {
        if (_prefs.getBytes(nvs_key_name, key_buffer, expected_len) == expected_len) {
            return true;
        }
        Serial.printf("%s Error reading '%s' from NVS despite correct length.\n", TAG,
                      nvs_key_name);
    } else if (len_in_nvs != 0) {
        Serial.printf("%s Found '%s' with incorrect length (%zu, expected %zu). Discarding.\n", TAG,
                      nvs_key_name, len_in_nvs, expected_len);
    }
    return false;
}

bool KeyStorage::storeKey(const char* nvs_key_name, const uint8_t* key_buffer, size_t len) {
    if (_prefs.putBytes(nvs_key_name, key_buffer, len) == len) {
        Serial.printf("%s Key '%s' stored successfully.\n", TAG, nvs_key_name);
        return true;
    }
    Serial.printf("%s CRITICAL: Failed to store key '%s'!\n", TAG, nvs_key_name);
    return false;
}

bool KeyStorage::manageEd25519KeyPair(uint8_t pk_out[POL_Ed25519_PK_SIZE],
                                      uint8_t sk_out[POL_Ed25519_SK_SIZE]) {
    bool pk_loaded = loadKey(NVS_Ed25519_PK_NAME, pk_out, POL_Ed25519_PK_SIZE);
    bool sk_loaded = loadKey(NVS_Ed25519_SK_NAME, sk_out, POL_Ed25519_SK_SIZE);

    if (pk_loaded && sk_loaded) {
        Serial.printf("%s Ed25519 PK and SK loaded from NVS.\n", TAG);
        return true;
    }

    Serial.printf(
        "%s  Ed25519 key pair not fully loaded from NVS or inconsistent. Generating new pair.\n",
        TAG);
    generateEd25519KeyPair(pk_out, sk_out);

    bool pk_stored = storeKey(NVS_Ed25519_PK_NAME, pk_out, POL_Ed25519_PK_SIZE);
    bool sk_stored = storeKey(NVS_Ed25519_SK_NAME, sk_out, POL_Ed25519_SK_SIZE);

    return pk_stored && sk_stored;  // Success if both parts of the new pair are stored
}

bool KeyStorage::manageX25519KeyPair(uint8_t pk_out[POL_X25519_PK_SIZE],
                                     uint8_t sk_out[POL_X25519_SK_SIZE]) {
    bool pk_loaded = loadKey(NVS_X25519_PK_NAME, pk_out, POL_X25519_PK_SIZE);
    bool sk_loaded = loadKey(NVS_X25519_SK_NAME, sk_out, POL_X25519_SK_SIZE);

    if (pk_loaded && sk_loaded) {
        Serial.printf("%s X25519 PK and SK loaded from NVS.\n", TAG);
        return true;
    }

    Serial.printf("%s X25519 key pair not fully loaded or inconsistent. Generating new pair.\n", TAG);
    generateX25519KeyPair(pk_out, sk_out);  // This function is from your crypto.cpp

    bool pk_stored = storeKey(NVS_X25519_PK_NAME, pk_out, POL_X25519_PK_SIZE);
    bool sk_stored = storeKey(NVS_X25519_SK_NAME, sk_out, POL_X25519_SK_SIZE);

    return pk_stored && sk_stored;
}

bool KeyStorage::manageServerX25519PublicKey(uint8_t pk_out[POL_X25519_PK_SIZE],
                                             const uint8_t hardcoded_pk[POL_X25519_PK_SIZE]) {
    Serial.printf("%s Managing Server's X25519 Public Key...\n", TAG);
    if (loadKey(NVS_SERVER_X25519_PK_NAME, pk_out, POL_X25519_PK_SIZE)) {
        Serial.printf("%s Server's X25519 PK loaded from NVS.\n", TAG);
        return true;
    }

    Serial.printf("%s Server's X25519 PK not found in NVS. Using hardcoded default.\n", TAG);
    memcpy(pk_out, hardcoded_pk, POL_X25519_PK_SIZE);

    if (storeKey(NVS_SERVER_X25519_PK_NAME, pk_out, POL_X25519_PK_SIZE)) {
        Serial.printf("%s Hardcoded Server's X25519 PK stored to NVS.\n", TAG);
    } else {
        Serial.printf("%s WARNING: Failed to store hardcoded Server's X25519 PK to NVS!\n", TAG);
        // Still return true because we have the hardcoded key available for use.
    }
    return true;  // Success because pk_out is populated (either from NVS or hardcoded)
}