#include "crypto.h"

#include <HardwareSerial.h>
#include <sodium.h>

// Generate a new Ed25519 key pair using Libsodium
void generateKeyPair(uint8_t public_key_out[POL_PK_SIZE], uint8_t secret_key_out[POL_SK_SIZE]) {
    if (crypto_sign_ed25519_keypair(public_key_out, secret_key_out) != 0) {
        Serial.println("[Crypto] Error: Libsodium keypair generation failed.");
        memset(public_key_out, 0, POL_PK_SIZE);
        memset(secret_key_out, 0, POL_SK_SIZE);
    }
}

// Verifies that the signature in `req` matches the signed fields
bool verifyPoLRequestSignature(const PoLRequest& req) {
    if (req.getSignedSize() != PoLRequest::SIGNED_SIZE) {
        Serial.println("[Crypto] Error: Request signed size mismatch.");
        return false;
    }

    uint8_t signed_data[PoLRequest::SIGNED_SIZE];
    req.getSignedData(signed_data);

    // crypto_sign_ed25519_verify_detached returns 0 on success, -1 on failure
    return crypto_sign_ed25519_verify_detached(req.phone_sig, signed_data, PoLRequest::SIGNED_SIZE,
                                               req.phone_pk) == 0;
}

// Sign a PoLResponse using the beacon's secret key with Libsodium
void signPoLResponse(PoLResponse& resp, const uint8_t secret_key[POL_SK_SIZE]) {
    if (resp.getSignedSize() == 0) {  // Or some other error check
        Serial.println("[Crypto] Error: Response signed size is zero.");
        return;
    }

    uint8_t buffer[PoLResponse::SIGNED_SIZE];
    resp.getSignedData(buffer);

    unsigned long long signature_actual_len;

    if (crypto_sign_ed25519_detached(resp.beacon_sig, &signature_actual_len, buffer,
                                     resp.getSignedSize(), secret_key) != 0) {
        Serial.println("[Crypto] Error: Libsodium signing PoLResponse failed.");
        memset(resp.beacon_sig, 0, crypto_sign_ed25519_BYTES);
    }
}

// Sign data specifically for periodic advertising broadcast with Libsodium
void signBeaconBroadcast(uint8_t signature_out[POL_SIG_SIZE], uint32_t beacon_id, uint64_t counter,
                         const uint8_t secret_key[POL_SK_SIZE]) {
    const size_t signed_data_len = sizeof(beacon_id) + sizeof(counter);
    uint8_t signed_data_buffer[signed_data_len];

    size_t offset = 0;
    memcpy(signed_data_buffer + offset, &beacon_id, sizeof(beacon_id));
    offset += sizeof(beacon_id);
    memcpy(signed_data_buffer + offset, &counter, sizeof(counter));

    unsigned long long signature_actual_len;

    if (crypto_sign_ed25519_detached(signature_out, &signature_actual_len, signed_data_buffer,
                                     signed_data_len, secret_key) != 0) {
        Serial.println("[Crypto] Error: Libsodium signing beacon broadcast failed.");
        memset(signature_out, 0, crypto_sign_ed25519_BYTES);
    }
}
