#ifndef CRYPTO_SERVICE_H
#define CRYPTO_SERVICE_H

#include <stdint.h>

#include "protocol/messages/pol_request.h"
#include "protocol/messages/pol_response.h"
#include "protocol/pol_constants.h"
#include "utils/key_manager.h"

class CryptoService {
public:
    CryptoService(const KeyManager& keyManager);

    bool verifyPoLRequestSignature(const PoLRequest& req) const;

    bool signPoLResponse(PoLResponse& resp, const PoLRequest& originalReq) const;

    bool signBeaconBroadcast(uint8_t signatureOut[SIG_SIZE], uint32_t beaconId,
                             uint64_t counter) const;

    // Encrypt data using ChaCha20-Poly1305 (IETF variant)
    // It will use the AEAD key fetched from KeyManager
    bool encryptAEAD(uint8_t ciphertextAndTagOut[], size_t& actualCiphertextLenOut,
                     const uint8_t plaintext[], size_t plaintextLen, const uint8_t associatedData[],
                     size_t associatedDataLen,
                     const uint8_t publicNonce[POL_AEAD_NONCE_SIZE]) const;

    // Decrypt data using ChaCha20-Poly1305 (IETF variant)
    // It will use the AEAD key fetched from KeyManager
    bool decryptAEAD(uint8_t plaintextOut[], size_t& actualPlaintextLenOut,
                     const uint8_t ciphertextAndTag[], size_t ciphertextAndTagLen,
                     const uint8_t associatedData[], size_t associatedDataLen,
                     const uint8_t publicNonce[POL_AEAD_NONCE_SIZE]) const;

private:
    const KeyManager& _keyManager;
    static constexpr const char* TAG = "[Crypto]";
};

#endif  // CRYPTO_SERVICE_H