#include "crypto.h"

#include <HardwareSerial.h>
#include <sodium.h>
#include <string.h>

#include "messages/pol_request.h"
#include "messages/pol_response.h"
#include "pol_constants.h"

// --- Ed25519 Signature Functions ---
void generateEd25519KeyPair(uint8_t public_key_out[Ed25519_PK_SIZE],
                            uint8_t secret_key_out[Ed25519_SK_SIZE]) {
    if (crypto_sign_ed25519_keypair(public_key_out, secret_key_out) != 0) {
        Serial.println("[Crypto] Error: Ed25519 keypair generation failed.");
        memset(public_key_out, 0, Ed25519_PK_SIZE);
        memset(secret_key_out, 0, Ed25519_SK_SIZE);
    }
}

bool verifyPoLRequestSignature(const PoLRequest& req) {
    if (req.getSignedSize() != PoLRequest::SIGNED_SIZE) {
        Serial.println("[Crypto] Error: Request signed size mismatch.");
        return false;
    }
    uint8_t signed_data[PoLRequest::SIGNED_SIZE];
    req.getSignedData(signed_data);
    unsigned long startTimeUs = micros();
    bool ok = crypto_sign_ed25519_verify_detached(req.phone_sig, signed_data,
                                                  PoLRequest::SIGNED_SIZE, req.phone_pk) == 0;
    unsigned long endTimeUs = micros();                             // Get end time in microseconds
    float durationMs = (float)(endTimeUs - startTimeUs) / 1000.0f;  // Convert to milliseconds

    Serial.printf("[Crypto] Signature verification took %.3f ms\n", durationMs);  // Approx 16 ms
    return ok;
}

void signPoLResponse(PoLResponse& resp, const uint8_t secret_key[Ed25519_SK_SIZE]) {
    if (resp.getSignedSize() == 0) {
        Serial.println("[Crypto] Error: Response signed size is zero.");
        memset(resp.beacon_sig, 0, SIG_SIZE);
        return;
    }
    uint8_t buffer[PoLResponse::SIGNED_SIZE];
    resp.getSignedData(buffer);
    unsigned long long sig_len;
    unsigned long startTimeUs = micros();
    if (crypto_sign_ed25519_detached(resp.beacon_sig, &sig_len, buffer, resp.getSignedSize(),
                                     secret_key) != 0) {
        Serial.println("[Crypto] Error: signing PoLResponse failed.");
        memset(resp.beacon_sig, 0, SIG_SIZE);
    }
    unsigned long endTimeUs = micros();                             // Get end time in microseconds
    float durationMs = (float)(endTimeUs - startTimeUs) / 1000.0f;  // Convert to milliseconds

    Serial.printf("[Crypto] Signature took %.3f ms\n", durationMs);  // approx 7ms (137ms on arduino -> x20 better)
}

void signBeaconBroadcast(uint8_t signature_out[SIG_SIZE], uint32_t beacon_id, uint64_t counter,
                         const uint8_t secret_key[Ed25519_SK_SIZE]) {
    const size_t signed_data_len = sizeof(beacon_id) + sizeof(counter);
    uint8_t signed_data_buffer[signed_data_len];
    size_t offset = 0;
    memcpy(signed_data_buffer + offset, &beacon_id, sizeof(beacon_id));
    offset += sizeof(beacon_id);
    memcpy(signed_data_buffer + offset, &counter, sizeof(counter));
    unsigned long long sig_len;
    if (crypto_sign_ed25519_detached(signature_out, &sig_len, signed_data_buffer, signed_data_len,
                                     secret_key) != 0) {
        Serial.println("[Crypto] Error: signing beacon broadcast failed.");
        memset(signature_out, 0, SIG_SIZE);
    }
}

// --- X25519 Key Agreement and AEAD Functions ---
void generateX25519KeyPair(uint8_t public_key_out[X25519_PK_SIZE],
                           uint8_t secret_key_out[X25519_SK_SIZE]) {
    // Generate 32 random bytes for the secret key
    randombytes_buf(secret_key_out, X25519_SK_SIZE);
    // Derive the public key from the secret key
    if (crypto_scalarmult_curve25519_base(public_key_out, secret_key_out) != 0) {
        Serial.println("[Crypto] Error: X25519 public key derivation failed.");
        memset(public_key_out, 0, X25519_PK_SIZE);
        // The secret_key_out is still random, which is fine for an X25519 sk.
    }
}

bool deriveAEADSharedKey(uint8_t shared_key_out[SHARED_KEY_SIZE],
                         const uint8_t my_x25519_secret_key[X25519_SK_SIZE],
                         const uint8_t their_x25519_public_key[X25519_PK_SIZE]) {
    if (crypto_scalarmult_curve25519(shared_key_out, my_x25519_secret_key,
                                     their_x25519_public_key) != 0) {
        Serial.println("[Crypto] Error: X25519 key agreement failed (possibly low-order key).");
        memset(shared_key_out, 0, SHARED_KEY_SIZE);
        return false;
    }
    return true;
}

bool encryptAEAD(uint8_t ciphertext_and_tag_out[], size_t& actual_ciphertext_len_out,
                 const uint8_t plaintext[], size_t plaintext_len, const uint8_t associated_data[],
                 size_t associated_data_len, const uint8_t public_nonce[POL_AEAD_NONCE_SIZE],
                 const uint8_t shared_key[SHARED_KEY_SIZE]) {
    unsigned long long ct_len_ull;
    if (crypto_aead_chacha20poly1305_ietf_encrypt(
            ciphertext_and_tag_out, &ct_len_ull, plaintext,
            (unsigned long long)plaintext_len,  // Cast size_t to ull
            associated_data, (unsigned long long)associated_data_len, NULL, public_nonce,
            shared_key) != 0) {
        Serial.println("[Crypto] Error: AEAD encryption failed.");
        actual_ciphertext_len_out = 0;
        return false;
    }
    actual_ciphertext_len_out = static_cast<size_t>(ct_len_ull);
    return true;
}

bool decryptAEAD(uint8_t plaintext_out[], size_t& actual_plaintext_len_out,
                 const uint8_t ciphertext_and_tag[], size_t ciphertext_and_tag_len,
                 const uint8_t associated_data[], size_t associated_data_len,
                 const uint8_t public_nonce[POL_AEAD_NONCE_SIZE],
                 const uint8_t shared_key[SHARED_KEY_SIZE]) {
    unsigned long long pt_len_ull;
    if (crypto_aead_chacha20poly1305_ietf_decrypt(
            plaintext_out, &pt_len_ull, NULL, ciphertext_and_tag,
            (unsigned long long)ciphertext_and_tag_len, associated_data,
            (unsigned long long)associated_data_len, public_nonce, shared_key) != 0) {
        Serial.println("[Crypto] Error: AEAD decryption failed (tag mismatch or bad data).");
        actual_plaintext_len_out = 0;
        return false;
    }
    actual_plaintext_len_out = static_cast<size_t>(pt_len_ull);
    return true;
}