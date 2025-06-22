package ch.drcookie.polaris_app.repository

enum class ScanCallbackType {
    FIRST_MATCH, // For finding a device to connect to
    ALL_MATCHES  // For continuous monitoring
}