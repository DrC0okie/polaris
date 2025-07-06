#include "crypto_service.h"

#include <HardwareSerial.h>
#include <sodium.h>
#include <string.h>

CryptoService::CryptoService(const KeyManager& keyManager) : _keyManager(keyManager) {
}

bool CryptoService::verifyPoLRequestSignature(const PoLRequest& req) const {
    if (req.getSignedSize() != PoLRequest::SIGNED_SIZE) {
        Serial.printf("%s Error: Request signed size mismatch. Expected %u, got %u\n", TAG,
                      PoLRequest::SIGNED_SIZE, req.getSignedSize());
        return false;
    }
    uint8_t signedData[PoLRequest::SIGNED_SIZE];
    req.getSignedData(signedData);

    return crypto_sign_ed25519_verify_detached(req.phoneSig, signedData, PoLRequest::SIGNED_SIZE,
                                               req.phonePk) == 0;
}

bool CryptoService::signPoLResponse(PoLResponse& resp, const PoLRequest& originalReq) const {
    const uint8_t* beaconSk = _keyManager.getEd25519Sk();
    if (!beaconSk) {
        Serial.printf("%s Error: Beacon Ed25519 SK not available for signing response.\n", TAG);
        return false;
    }

    uint8_t
        bufferToSign[PoLResponse::SIGNED_SIZE];  // Buffer for the data that will actually be signed
    resp.getSignedData(bufferToSign,
                       originalReq);  // Pass originalReq to construct the full signed data
    unsigned long long signatureLengthOut;

    if (crypto_sign_ed25519_detached(resp.beaconSig, &signatureLengthOut, bufferToSign,
                                     PoLResponse::SIGNED_SIZE, beaconSk) != 0) {
        Serial.printf("%s Error: signing PoLResponse failed.\n", TAG);
        memset(resp.beaconSig, 0, SIG_SIZE);
        return false;
    }
    return true;
}

bool CryptoService::signBeaconBroadcast(uint8_t signatureOut[SIG_SIZE], uint32_t beaconId,
                                        uint64_t counter) const {
    const uint8_t* beaconSk = _keyManager.getEd25519Sk();
    if (!beaconSk) {
        Serial.printf("%s Error: Beacon Ed25519 SK not available for broadcast signing.\n", TAG);
        return false;
    }

    const size_t signedDataLen = sizeof(beaconId) + sizeof(counter);
    uint8_t signedDataBuffer[signedDataLen];
    size_t offset = 0;
    memcpy(signedDataBuffer + offset, &beaconId, sizeof(beaconId));
    offset += sizeof(beaconId);
    memcpy(signedDataBuffer + offset, &counter, sizeof(counter));

    unsigned long long signatureLengthOut;
    if (crypto_sign_ed25519_detached(signatureOut, &signatureLengthOut, signedDataBuffer,
                                     signedDataLen, beaconSk) != 0) {
        Serial.printf("%s Error: signing beacon broadcast failed.\n", TAG);
        memset(signatureOut, 0, SIG_SIZE);
        return false;
    }
    return true;
}

bool CryptoService::encryptAEAD(uint8_t ciphertextAndTagOut[], size_t& actualCiphertextLenOut,
                                const uint8_t plaintext[], size_t plaintextLen,
                                const uint8_t associatedData[], size_t associatedDataLen,
                                const uint8_t publicNonce[POL_AEAD_NONCE_SIZE]) const {
    const uint8_t* sharedKeyWithServer = _keyManager.getAeadKey();
    if (!sharedKeyWithServer) {
        Serial.printf("%s Error: Shared AEAD key not available for encryption.\n", TAG);
        actualCiphertextLenOut = 0;
        return false;
    }

    unsigned long long cyphertextLength;
    if (crypto_aead_chacha20poly1305_ietf_encrypt(ciphertextAndTagOut, &cyphertextLength, plaintext,
                                                  (unsigned long long)plaintextLen, associatedData,
                                                  (unsigned long long)associatedDataLen, NULL,
                                                  publicNonce, sharedKeyWithServer) != 0) {
        Serial.printf("%s Error: AEAD encryption failed.\n", TAG);
        actualCiphertextLenOut = 0;
        return false;
    }
    actualCiphertextLenOut = static_cast<size_t>(cyphertextLength);
    return true;
}

bool CryptoService::decryptAEAD(uint8_t plaintextOut[], size_t& actualPlaintextLenOut,
                                const uint8_t ciphertextAndTag[], size_t ciphertextAndTagLen,
                                const uint8_t associatedData[], size_t associatedDataLen,
                                const uint8_t publicNonce[POL_AEAD_NONCE_SIZE],
                                const uint8_t* overrideKey) const {
    const uint8_t* keyToUse = overrideKey;
    if (keyToUse == nullptr) {
        keyToUse = _keyManager.getAeadKey();
    }

    if (!keyToUse) {
        Serial.printf("%s Error: Shared AEAD key not available for decryption.\n", TAG);
        actualPlaintextLenOut = 0;
        return false;
    }

    unsigned long long plaintextLength;
    if (crypto_aead_chacha20poly1305_ietf_decrypt(
            plaintextOut, &plaintextLength, NULL, ciphertextAndTag,
            (unsigned long long)ciphertextAndTagLen, associatedData,
            (unsigned long long)associatedDataLen, publicNonce, keyToUse) != 0) {
        Serial.printf("%s Error: AEAD decryption failed (tag mismatch or bad data).\n", TAG);
        actualPlaintextLenOut = 0;
        return false;
    }
    actualPlaintextLenOut = static_cast<size_t>(plaintextLength);
    return true;
}