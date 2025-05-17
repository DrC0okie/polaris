#ifndef CRYPTO_H
#define CRYPTO_H

#include <stdint.h>

#include "messages/pol_request.h"
#include "messages/pol_response.h"

// Forward declarations
class PoLRequest;
class PoLResponse;
class EncryptedPayloadOut;
class EncryptedPayloadIn;

// --- Ed25519 Signature Functions ---

// Generate a new Ed25519 key pair
void generateEd25519KeyPair(uint8_t publicKeyOut[Ed25519_PK_SIZE],
                            uint8_t secretKeyOut[Ed25519_SK_SIZE]);

// Verifies the signature in a PoLRequest using Monocypher
// Returns true if signature is valid
bool verifyPoLRequestSignature(const PoLRequest& req);

// Sign a PoLResponse using the beacon's secret key
void signPoLResponse(PoLResponse& resp, const PoLRequest& original_req,
                     const uint8_t secretKey[Ed25519_SK_SIZE]);

// Sign data specifically for periodic advertising broadcast
void signBeaconBroadcast(uint8_t signatureOut[SIG_SIZE], uint32_t beaconId, uint64_t counter,
                         const uint8_t secretKey[Ed25519_SK_SIZE]);

// -- -X25519 Key Agreement and AEAD Functions-- -

// Generate a new X25519 key pair (for encryption/decryption)
void generateX25519KeyPair(uint8_t publicKeyOut[X25519_PK_SIZE],
                           uint8_t secretKeyOut[X25519_SK_SIZE]);

// Derive a shared secret using our X25519 secret key and their X25519 public key
// This raw shared secret will be used as the AEAD key.
bool deriveAEADSharedKey(uint8_t sharedKeyOut[SHARED_KEY_SIZE],
                         const uint8_t x25519SecretKey[X25519_SK_SIZE],
                         const uint8_t serverX25519PublicKey[X25519_PK_SIZE]);

// Encrypt data using ChaCha20-Poly1305 (IETF variant)
bool encryptAEAD(uint8_t ciphertextAndTagOut[], size_t& actualCiphertextLenOut,
                 const uint8_t plaintext[], size_t plaintextLen, const uint8_t associatedData[],
                 size_t associatedDataLen, const uint8_t publicNonce[POL_AEAD_NONCE_SIZE],
                 const uint8_t sharedKey[SHARED_KEY_SIZE]);

// Decrypt data using ChaCha20-Poly1305 (IETF variant)
bool decryptAEAD(uint8_t plaintextOut[], size_t& actualPlaintextLenOut,
                 const uint8_t ciphertextAndTag[], size_t ciphertextAndTagLen,
                 const uint8_t associatedData[], size_t associatedDataLen,
                 const uint8_t publicNonce[POL_AEAD_NONCE_SIZE],
                 const uint8_t sharedKey[SHARED_KEY_SIZE]);

#endif
