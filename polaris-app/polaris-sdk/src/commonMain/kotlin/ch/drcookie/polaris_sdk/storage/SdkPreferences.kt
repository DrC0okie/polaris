package ch.drcookie.polaris_sdk.storage

public interface SdkPreferences {
    public var apiKey: String?
    public var phoneId: Long
    public var phonePublicKey: String?
    public var phoneSecretKey: String?
}