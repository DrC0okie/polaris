#ifndef CRYPTO_H
#define CRYPTO_H

#include <stdint.h>

#include "pol_request.h"
#include "pol_response.h"

// Verifies the signature in a PoLRequest using Monocypher
// Returns true if signature is valid
bool verifyPoLRequestSignature(const PoLRequest& req);

// Generate a new Ed25519 key pair
void generateKeyPair(uint8_t public_key[32], uint8_t secret_key[32]);

// Sign a PoLResponse using the beacon's secret key
void signPoLResponse(PoLResponse& resp, const uint8_t secret_key[32],
                     const uint8_t public_key[32]);

// Sign data specifically for periodic advertising broadcast
void signBeaconBroadcast(uint8_t signature_out[64], uint32_t beacon_id,
                         uint64_t counter, const uint8_t secret_key[32],
                         const uint8_t public_key[32]);

#endif
