#ifndef CRYPTO_H
#define CRYPTO_H

#include <stdint.h>

#include "pol_request.h"
#include "pol_response.h"

// Forward declarations
class PoLRequest;
class PoLResponse;
class EncryptedPayloadOut;
class EncryptedPayloadIn;

// --- Ed25519 Signature Functions ---

// Generate a new Ed25519 key pair
void generateEd25519KeyPair(uint8_t public_key_out[POL_Ed25519_PK_SIZE], uint8_t secret_key_out[POL_Ed25519_SK_SIZE]);

// Verifies the signature in a PoLRequest using Monocypher
// Returns true if signature is valid
bool verifyPoLRequestSignature(const PoLRequest& req);

// Sign a PoLResponse using the beacon's secret key
void signPoLResponse(PoLResponse& resp, const uint8_t secret_key[POL_Ed25519_SK_SIZE]);

// Sign data specifically for periodic advertising broadcast
void signBeaconBroadcast(uint8_t signature_out[POL_SIG_SIZE], uint32_t beacon_id, uint64_t counter,
                         const uint8_t secret_key[POL_Ed25519_SK_SIZE]);

// -- -X25519 Key Agreement and AEAD Functions-- -

// Generate a new X25519 key pair (for encryption/decryption)
void generateX25519KeyPair(uint8_t public_key_out[POL_X25519_PK_SIZE],
                           uint8_t secret_key_out[POL_X25519_SK_SIZE]);

// Derive a shared secret using our X25519 secret key and their X25519 public key
// This raw shared secret will be used as the AEAD key.
bool deriveAEADSharedKey(uint8_t shared_key_out[POL_SHARED_KEY_SIZE],
                         const uint8_t my_x25519_secret_key[POL_X25519_SK_SIZE],
                         const uint8_t their_x25519_public_key[POL_X25519_PK_SIZE]);

// Encrypt data using ChaCha20-Poly1305 (IETF variant)
bool encryptAEAD(uint8_t ciphertext_and_tag_out[], size_t& actual_ciphertext_len_out,
                 const uint8_t plaintext[], size_t plaintext_len, const uint8_t associated_data[],
                 size_t associated_data_len,
                 const uint8_t public_nonce[POL_AEAD_NONCE_SIZE],
                 const uint8_t shared_key[POL_SHARED_KEY_SIZE]);

// Decrypt data using ChaCha20-Poly1305 (IETF variant)
bool decryptAEAD(uint8_t plaintext_out[], size_t& actual_plaintext_len_out,
                 const uint8_t ciphertext_and_tag[], size_t ciphertext_and_tag_len,
                 const uint8_t associated_data[], size_t associated_data_len,
                 const uint8_t public_nonce[POL_AEAD_NONCE_SIZE],
                 const uint8_t shared_key[POL_SHARED_KEY_SIZE]);

#endif
