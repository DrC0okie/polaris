#ifndef CRYPTO_SERVICE_H
#define CRYPTO_SERVICE_H

#include <stdint.h>

#include "protocol/messages/pol_request.h"
#include "protocol/messages/pol_response.h"
#include "protocol/pol_constants.h"
#include "utils/key_manager.h"

/**
 * @class CryptoService
 * @brief Provides a high-level interface for all cryptographic operations.
 *
 * This class acts as a facade for the libsodium library, handling signatures and
 * authenticated encryption/decryption (AEAD).
 * It relies on a KeyManager instance to provide the necessary cryptographic keys.
 */
class CryptoService {
public:
    /**
     * @brief Constructs the CryptoService.
     * @param keyManager A reference to an initialized KeyManager instance.
     */
    CryptoService(const KeyManager& keyManager);

    /**
     * @brief Verifies the Ed25519 signature of a Proof-of-Location request.
     * @param req The PoLRequest object containing the data and signature to verify.
     * @return True if the signature is valid, false otherwise.
     */
    bool verifyPoLRequestSignature(const PoLRequest& req) const;

    /**
     * @brief Signs a Proof-of-Location response using the beaco Ed25519 private key.
     * @param resp The PoLResponse object to be signed. The signature is written into its
     * `beaconSig` field.
     * @param originalReq The original request, used to provide context for the signature.
     * @return True if signing was successful, false otherwise.
     */
    bool signPoLResponse(PoLResponse& resp, const PoLRequest& originalReq) const;

    /**
     * @brief Signs the beacon broadcast data (ID and counter) for extended advertising.
     * @param signatureOut Buffer where the resulting 64-byte Ed25519 signature will be written.
     * @param beaconId The beacon ID to be included in the signature.
     * @param counter The current counter value to be included in the signature.
     * @return True if signing was successful, false otherwise.
     */
    bool signBeaconBroadcast(uint8_t signatureOut[SIG_SIZE], uint32_t beaconId,
                             uint64_t counter) const;

    /**
     * @brief Encrypts and authenticates a plaintext message using ChaCha20-Poly1305.
     *
     * This function uses the pre-derived shared key with the server for encryption.
     *
     * @param ciphertextAndTagOut Buffer to store the resulting ciphertext and authentication tag.
     * @param actualCiphertextLenOut Reference to store the actual length of the output.
     * @param plaintext The data to encrypt.
     * @param plaintextLen The length of the plaintext data.
     * @param associatedData Additional data to be authenticated but not encrypted.
     * @param associatedDataLen The length of the associated data.
     * @param publicNonce A unique nonce for this specific encryption operation.
     * @return True if encryption was successful, false otherwise.
     */
    bool encryptAEAD(uint8_t ciphertextAndTagOut[], size_t& actualCiphertextLenOut,
                     const uint8_t plaintext[], size_t plaintextLen, const uint8_t associatedData[],
                     size_t associatedDataLen,
                     const uint8_t publicNonce[POL_AEAD_NONCE_SIZE]) const;

    /**
     * @brief Decrypts and verifies an authenticated ciphertext message using ChaCha20-Poly1305.
     *
     * This function uses the pre-derived shared key with the server for decryption.
     * It will only succeed if the authentication tag is valid for the key, nonce,
     * associated data, and ciphertext.
     *
     * @param plaintextOut Buffer to store the resulting decrypted plaintext.
     * @param actualPlaintextLenOut Reference to store the actual length of the decrypted data.
     * @param ciphertextAndTag The encrypted data and tag to decrypt.
     * @param ciphertextAndTagLen The length of the encrypted data and tag.
     * @param associatedData Additional data. Must match the data used during encryption.
     * @param associatedDataLen The length of the associated data.
     * @param publicNonce The public nonce used during the encryption operation.
     * @return True if decryption and verification were successful, false otherwise.
     */
    bool decryptAEAD(uint8_t plaintextOut[], size_t& actualPlaintextLenOut,
                     const uint8_t ciphertextAndTag[], size_t ciphertextAndTagLen,
                     const uint8_t associatedData[], size_t associatedDataLen,
                     const uint8_t publicNonce[POL_AEAD_NONCE_SIZE],
                     const uint8_t* overrideKey = nullptr) const;

private:
    /// @brief A reference to the key manager that provides all cryptographic keys.
    const KeyManager& _keyManager;

    /// @brief A tag used for logging from this class.
    static constexpr const char* TAG = "[Crypto]";
};

#endif  // CRYPTO_SERVICE_H