package ch.drcookie.polaris_sdk.domain.repository

interface LocalPreferences {
    var apiKey: String?
    var phoneId: Long
    var phonePublicKey: String?
    var phoneSecretKey: String?
}