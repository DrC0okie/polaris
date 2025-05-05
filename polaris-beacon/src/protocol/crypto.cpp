#include "crypto.h"
extern "C" {
#include "monocypher.h"
}
#include <HardwareSerial.h>
#include <esp_random.h>

void generateKeyPair(uint8_t public_key[32], uint8_t secret_key[32]) {
    // Fill secret key with 32 bytes of true random data
    esp_fill_random(secret_key, 32);

    // Derive the Ed25519 public key
    crypto_sign_public_key(public_key, secret_key);
}

// Verifies that the signature in `req` matches the signed fields using the
// phone's public key
bool verifyPoLRequestSignature(const PoLRequest& req) {
    if (req.getSignedSize() != PoLRequest::SIGNED_SIZE) {
        Serial.println("[Crypto] Error: Request signed size mismatch.");
        return false;
    }

    uint8_t signed_data[PoLRequest::SIGNED_SIZE];
    req.getSignedData(signed_data);
    return crypto_check(req.phone_sig, req.phone_pk, signed_data,
                        PoLRequest::SIGNED_SIZE) == 0;
}

void signPoLResponse(PoLResponse& resp, const uint8_t secret_key[32],
                     const uint8_t public_key[32]) {
    uint8_t buffer[resp.getSignedSize()];
    resp.getSignedData(buffer);
    crypto_sign(resp.beacon_sig, secret_key, public_key, buffer,
                sizeof(buffer));
}

void signBeaconBroadcast(uint8_t signature_out[64], uint32_t beacon_id,
                         uint64_t counter, const uint8_t secret_key[32],
                         const uint8_t public_key[32]) {
    // Buffer to hold the data being signed
    const size_t signed_data_len = sizeof(beacon_id) + sizeof(counter);
    uint8_t signed_data_buffer[signed_data_len];

    // Serialize data into the buffer
    size_t offset = 0;
    memcpy(signed_data_buffer + offset, &beacon_id, sizeof(beacon_id));
    offset += sizeof(beacon_id);
    memcpy(signed_data_buffer + offset, &counter, sizeof(counter));

    // Perform the signature
    crypto_sign(signature_out, secret_key, public_key, signed_data_buffer,
                signed_data_len);
}
