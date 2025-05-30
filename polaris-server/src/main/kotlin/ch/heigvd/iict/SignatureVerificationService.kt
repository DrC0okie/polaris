package ch.heigvd.iict

import jakarta.enterprise.context.ApplicationScoped


@ApplicationScoped
class SignatureVerificationService {

    // Placeholder for actual beacon public key retrieval
    @OptIn(ExperimentalUnsignedTypes::class)
    private fun getBeaconPublicKey(beaconId: UInt): UByteArray? {
        if (beaconId == 1u) { // Assuming BEACON_ID = 1
            return "A3540D31912B89B101B4FA69F37ACFA49E3B1BAA0D1D04C8202BFD1B20B741D3"
                .chunked(2).map { it.toInt(16).toUByte() }.toUByteArray()
        }
        return null
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun verifyToken(token: PoLToken): Boolean {
        // 1. Verify phone's signature
        val phoneSignedData = token.getPhoneSignedData()
        val isPhoneSigValid = verifyDetached(token.phoneSig, phoneSignedData, token.phonePk)
        if (!isPhoneSigValid) {
            println("Phone signature invalid for phoneId: ${token.phoneId}")
            return false
        }
        println("Phone signature VALID for phoneId: ${token.phoneId}")

        // 2. Verify beacon's signature
        val beaconPublicKey = getBeaconPublicKey(token.beaconId)
        if (beaconPublicKey == null) {
            println("Beacon public key not found for beaconId: ${token.beaconId}")
            return false
        }
        val beaconSignedData = token.getBeaconEffectivelySignedData()
        val isBeaconSigValid = verifyDetached(token.beaconSig, beaconSignedData, beaconPublicKey)
        if (!isBeaconSigValid) {
            println("Beacon signature invalid for beaconId: ${token.beaconId}")
            return false
        }
        println("Beacon signature VALID for beaconId: ${token.beaconId}")

        // Potentially other checks: nonce freshness, counter monotonicity, etc.

        return true
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun verifyDetached(signature: UByteArray, message: UByteArray, publicKey: UByteArray): Boolean {
        // TODO: Replace with actual Libsodium call via crypto library
        // return Sodium.crypto_sign_ed25519_verify_detached(signature.asByteArray(), message.asByteArray(), message.size.toLong(), publicKey.asByteArray()) == 0

        // For now, a placeholder.
        println("Placeholder: Verifying sig (${signature.size}B) for msg (${message.size}B) with PK (${publicKey.size}B)")
        if (signature.isEmpty() || message.isEmpty() || publicKey.isEmpty()) return false // Basic check
        return true // Placeholder: Assume valid for now for PoC flow
    }
}