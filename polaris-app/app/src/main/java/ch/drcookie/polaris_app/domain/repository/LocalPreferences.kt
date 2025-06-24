package ch.drcookie.polaris_app.domain.repository

interface LocalPreferences {
    var apiKey: String?
    var phoneId: Long
    var phonePublicKey: String?
    var phoneSecretKey: String?
}