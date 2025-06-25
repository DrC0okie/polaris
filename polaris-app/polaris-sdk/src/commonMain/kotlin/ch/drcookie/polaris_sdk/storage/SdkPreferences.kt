package ch.drcookie.polaris_sdk.storage

interface SdkPreferences {
    var apiKey: String?
    var phoneId: Long
    var phonePublicKey: String?
    var phoneSecretKey: String?
}