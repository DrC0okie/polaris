#include "crypto.h"

#include <HardwareSerial.h>
#include <sodium.h>
#include <string.h>

#include "messages/pol_request.h"
#include "messages/pol_response.h"
#include "pol_constants.h"

// --- Ed25519 Signature Functions ---
void generateEd25519KeyPair(uint8_t publicKeyOut[Ed25519_PK_SIZE],
                            uint8_t secretKeyOut[Ed25519_SK_SIZE]) {
    if (crypto_sign_ed25519_keypair(publicKeyOut, secretKeyOut) != 0) {
        Serial.println("[Crypto] Error: Ed25519 keypair generation failed.");
        memset(publicKeyOut, 0, Ed25519_PK_SIZE);
        memset(secretKeyOut, 0, Ed25519_SK_SIZE);
    }
}

bool verifyPoLRequestSignature(const PoLRequest& req) {
    if (req.getSignedSize() != PoLRequest::SIGNED_SIZE) {
        Serial.println("[Crypto] Error: Request signed size mismatch.");
        return false;
    }
    uint8_t signedData[PoLRequest::SIGNED_SIZE];
    req.getSignedData(signedData);
    return crypto_sign_ed25519_verify_detached(req.phoneSig, signedData, PoLRequest::SIGNED_SIZE,
                                               req.phonePk) == 0;
}

void signPoLResponse(PoLResponse& resp, const PoLRequest& originalRequest,
                     const uint8_t secretKey[Ed25519_SK_SIZE]) {
    uint8_t buffer[PoLResponse::SIGNED_SIZE];
    resp.getSignedData(buffer, originalRequest);
    unsigned long long signatureLength;

    if (crypto_sign_ed25519_detached(resp.beaconSig, &signatureLength, buffer, resp.getSignedSize(),
                                     secretKey) != 0) {
        Serial.println("[Crypto] Error: signing PoLResponse failed.");
        memset(resp.beaconSig, 0, SIG_SIZE);
    }
}

void signBeaconBroadcast(uint8_t signatureOut[SIG_SIZE], uint32_t beaconId, uint64_t counter,
                         const uint8_t secretKey[Ed25519_SK_SIZE]) {
    const size_t signed_data_len = sizeof(beaconId) + sizeof(counter);
    uint8_t signedDataBuffer[signed_data_len];
    size_t offset = 0;
    memcpy(signedDataBuffer + offset, &beaconId, sizeof(beaconId));
    offset += sizeof(beaconId);
    memcpy(signedDataBuffer + offset, &counter, sizeof(counter));
    unsigned long long signatureLength;
    if (crypto_sign_ed25519_detached(signatureOut, &signatureLength, signedDataBuffer,
                                     signed_data_len, secretKey) != 0) {
        Serial.println("[Crypto] Error: signing beacon broadcast failed.");
        memset(signatureOut, 0, SIG_SIZE);
    }
}

// --- X25519 Key Agreement and AEAD Functions ---
void generateX25519KeyPair(uint8_t publicKeyOut[X25519_PK_SIZE],
                           uint8_t secretKeyOut[X25519_SK_SIZE]) {
    // Generate 32 random bytes for the secret key
    randombytes_buf(secretKeyOut, X25519_SK_SIZE);
    // Derive the public key from the secret key
    if (crypto_scalarmult_curve25519_base(publicKeyOut, secretKeyOut) != 0) {
        Serial.println("[Crypto] Error: X25519 public key derivation failed.");
        memset(publicKeyOut, 0, X25519_PK_SIZE);
        // The secret_key_out is still random, which is fine for an X25519 sk.
    }
}

bool deriveAEADSharedKey(uint8_t sharedKeyOut[SHARED_KEY_SIZE],
                         const uint8_t X25519SecretKey[X25519_SK_SIZE],
                         const uint8_t serverX25519PublicKey[X25519_PK_SIZE]) {
    if (crypto_scalarmult_curve25519(sharedKeyOut, X25519SecretKey, serverX25519PublicKey) != 0) {
        Serial.println("[Crypto] Error: X25519 key agreement failed (possibly low-order key).");
        memset(sharedKeyOut, 0, SHARED_KEY_SIZE);
        return false;
    }
    return true;
}

bool encryptAEAD(uint8_t ciphertextAndTagOut[], size_t& actualCiphertextLenOut,
                 const uint8_t plaintext[], size_t plaintextLen, const uint8_t associatedData[],
                 size_t associatedDataLen, const uint8_t publicNonce[POL_AEAD_NONCE_SIZE],
                 const uint8_t sharedKey[SHARED_KEY_SIZE]) {
    unsigned long long cyphertextLength;
    if (crypto_aead_chacha20poly1305_ietf_encrypt(
            ciphertextAndTagOut, &cyphertextLength, plaintext,
            (unsigned long long)plaintextLen,  // Cast size_t to ull
            associatedData, (unsigned long long)associatedDataLen, NULL, publicNonce,
            sharedKey) != 0) {
        Serial.println("[Crypto] Error: AEAD encryption failed.");
        actualCiphertextLenOut = 0;
        return false;
    }
    actualCiphertextLenOut = static_cast<size_t>(cyphertextLength);
    return true;
}

bool decryptAEAD(uint8_t plaintextOut[], size_t& actualPlaintextLenOut,
                 const uint8_t ciphertextAndTag[], size_t ciphertextAndTagLen,
                 const uint8_t associatedData[], size_t associatedDataLen,
                 const uint8_t publicNonce[POL_AEAD_NONCE_SIZE],
                 const uint8_t sharedKey[SHARED_KEY_SIZE]) {
    unsigned long long plaintextLength;
    if (crypto_aead_chacha20poly1305_ietf_decrypt(
            plaintextOut, &plaintextLength, NULL, ciphertextAndTag,
            (unsigned long long)ciphertextAndTagLen, associatedData,
            (unsigned long long)associatedDataLen, publicNonce, sharedKey) != 0) {
        Serial.println("[Crypto] Error: AEAD decryption failed (tag mismatch or bad data).");
        actualPlaintextLenOut = 0;
        return false;
    }
    actualPlaintextLenOut = static_cast<size_t>(plaintextLength);
    return true;
}