#include "crypto.h"
extern "C" {
#include "monocypher.h"
}
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
    size_t len = req.getSignedSize();
    uint8_t* signed_data = new uint8_t[len];

    req.getSignedData(signed_data);

    if (!signed_data) {
        return false;
    }

    bool ok = crypto_check(req.phone_sig, req.phone_pk, signed_data, len) == 0;

    delete[] signed_data;
    return ok;
}

void signPoLResponse(PoLResponse& resp, const uint8_t secret_key[32],
                     const uint8_t public_key[32]) {
    uint8_t buffer[resp.getSignedSize()];
    resp.getSignedData(buffer);
    crypto_sign(resp.beacon_sig, secret_key, public_key, buffer,
                sizeof(buffer));
}
