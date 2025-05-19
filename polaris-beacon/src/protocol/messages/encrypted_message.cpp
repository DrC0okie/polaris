#include "encrypted_message.h"

#include <HardwareSerial.h>
#include <sodium.h>
#include <string.h>

#include "../crypto.h"

// --- InnerPlaintext Implementation ---
size_t InnerPlaintext::getTotalSerializedSize() const {
    return sizeof(msgId) + sizeof(msgType) + sizeof(opType) + sizeof(beaconCnt) +
           sizeof(actualPayloadLength) + actualPayloadLength;
}

size_t InnerPlaintext::serialize(uint8_t* buffer) const {
    size_t offset = 0;
    memcpy(buffer + offset, &msgId, sizeof(msgId));
    offset += sizeof(msgId);
    memcpy(buffer + offset, &msgType, sizeof(msgType));
    offset += sizeof(msgType);
    memcpy(buffer + offset, &opType, sizeof(opType));
    offset += sizeof(opType);
    memcpy(buffer + offset, &beaconCnt, sizeof(beaconCnt));
    offset += sizeof(beaconCnt);
    memcpy(buffer + offset, &actualPayloadLength, sizeof(actualPayloadLength));
    offset += sizeof(actualPayloadLength);
    memcpy(buffer + offset, payload, actualPayloadLength);
    offset += actualPayloadLength;
    return offset;
}

bool InnerPlaintext::deserialize(const uint8_t* buffer, size_t len) {
    size_t expectedMinLen = sizeof(msgId) + sizeof(msgType) + sizeof(opType) + sizeof(beaconCnt) +
                            sizeof(actualPayloadLength);
    if (len < expectedMinLen)
        return false;

    size_t offset = 0;
    memcpy(&msgId, buffer + offset, sizeof(msgId));
    offset += sizeof(msgId);
    memcpy(&msgType, buffer + offset, sizeof(msgType));
    offset += sizeof(msgType);
    memcpy(&opType, buffer + offset, sizeof(opType));
    offset += sizeof(opType);
    memcpy(&beaconCnt, buffer + offset, sizeof(beaconCnt));
    offset += sizeof(beaconCnt);
    memcpy(&actualPayloadLength, buffer + offset, sizeof(actualPayloadLength));
    offset += sizeof(actualPayloadLength);

    if (len < expectedMinLen + actualPayloadLength)
        return false;  // Check total length
    if (actualPayloadLength > sizeof(payload))
        return false;  // Check buffer overflow

    memcpy(payload, buffer + offset, actualPayloadLength);
    return true;
}

// --- EncryptedMessage Implementation ---
EncryptedMessage::EncryptedMessage() : ciphertextWithTagLen(0) {
    memset(nonce, 0, sizeof(nonce));
    memset(ciphertextWithTag, 0, sizeof(ciphertextWithTag));
    beaconIdAd = 0;
}

bool EncryptedMessage::seal(const InnerPlaintext& innerPt, uint32_t senderBeaconIdAd,
                            const uint8_t sharedKey[SHARED_KEY_SIZE]) {
    this->beaconIdAd = senderBeaconIdAd;

    // Generate a unique 12-byte nonce for this message
    //    IMPORTANT: This nonce MUST be unique for each message encrypted with the same key.
    //    For a beacon, a simple incrementing counter stored in NVS
    //    could work, or random bytes. For PoC, random is fine.
    randombytes_buf(this->nonce, POL_AEAD_NONCE_SIZE);

    // Serialize InnerPlaintext
    uint8_t innerPlaintextBuffer[MAX_INNER_PLAINTEXT_SIZE];
    size_t innerPlaintextLen = innerPt.serialize(innerPlaintextBuffer);
    if (innerPlaintextLen == 0 || innerPlaintextLen > MAX_INNER_PLAINTEXT_SIZE) {
        Serial.println("[EncMsg] Seal: Inner plaintext serialization error or too large.");
        return false;
    }

    // Prepare Associated Data (beaconId)
    uint8_t adBuffer[sizeof(uint32_t)];
    memcpy(adBuffer, &this->beaconIdAd, sizeof(this->beaconIdAd));

    // Encrypt
    if (!encryptAEAD(this->ciphertextWithTag, this->ciphertextWithTagLen, innerPlaintextBuffer,
                     innerPlaintextLen, adBuffer, sizeof(adBuffer), this->nonce, sharedKey)) {
        Serial.println("[EncMsg] Seal: AEAD encryption failed.");
        return false;
    }

    return true;
}

bool EncryptedMessage::unseal(InnerPlaintext& innerPtOut,
                              const uint8_t sharedKey[SHARED_KEY_SIZE]) {
    // Assumes beaconIdAd, nonce, ciphertextWithTag, and ciphertextWithTagLen
    // have been populated by fromBytes()

    // Prepare Associated Data
    uint8_t adBuffer[sizeof(uint32_t)];
    memcpy(adBuffer, &this->beaconIdAd, sizeof(this->beaconIdAd));

    // Decrypt
    uint8_t decryptedInnerPlaintextBuffer[MAX_INNER_PLAINTEXT_SIZE];
    size_t decryptedInnerPlaintextLen = 0;

    if (!decryptAEAD(decryptedInnerPlaintextBuffer, decryptedInnerPlaintextLen,
                     this->ciphertextWithTag, this->ciphertextWithTagLen, adBuffer,
                     sizeof(adBuffer), this->nonce, sharedKey)) {
        Serial.println("[EncMsg] Unseal: AEAD decryption failed (bad tag or data).");
        return false;
    }

    // Deserialize InnerPlaintext
    if (!innerPtOut.deserialize(decryptedInnerPlaintextBuffer, decryptedInnerPlaintextLen)) {
        Serial.println("[EncMsg] Unseal: Failed to deserialize inner plaintext.");
        return false;
    }
    return true;
}

size_t EncryptedMessage::packedSize() const {
    return sizeof(beaconIdAd) + POL_AEAD_NONCE_SIZE + ciphertextWithTagLen;
}

size_t EncryptedMessage::toBytes(uint8_t* buffer, size_t bufferMaxLen) const {
    size_t totalLen = packedSize();
    if (bufferMaxLen < totalLen)
        return 0;  // Not enough space

    size_t offset = 0;
    memcpy(buffer + offset, &beaconIdAd, sizeof(beaconIdAd));
    offset += sizeof(beaconIdAd);
    memcpy(buffer + offset, nonce, POL_AEAD_NONCE_SIZE);
    offset += POL_AEAD_NONCE_SIZE;
    memcpy(buffer + offset, ciphertextWithTag, ciphertextWithTagLen);
    offset += ciphertextWithTagLen;
    return offset;
}

bool EncryptedMessage::fromBytes(const uint8_t* data, size_t len) {
    size_t minHeaderLen = sizeof(beaconIdAd) + POL_AEAD_NONCE_SIZE;
    if (len < minHeaderLen)
        return false;

    size_t offset = 0;
    memcpy(&beaconIdAd, data + offset, sizeof(beaconIdAd));
    offset += sizeof(beaconIdAd);
    memcpy(nonce, data + offset, POL_AEAD_NONCE_SIZE);
    offset += POL_AEAD_NONCE_SIZE;

    this->ciphertextWithTagLen = len - offset;
    if (this->ciphertextWithTagLen > MAX_CIPHERTEXT_WITH_TAG_SIZE)
        return false;  // Too large for buffer

    memcpy(ciphertextWithTag, data + offset, this->ciphertextWithTagLen);
    return true;
}