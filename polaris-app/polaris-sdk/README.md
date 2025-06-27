# Polaris SDK

A Kotlin Multiplatform SDK for interacting with  Polaris-compatible BLE beacons. This SDK provides a complete toolkit for Proof-of-Location (PoL), secure payload delivery, and  broadcast monitoring.

Built with a configuration-driven, and type-safe API, the Polaris SDK is designed to be both easy to use for standard applications and  flexible enough for custom implementations.

## Features

- Cross-Platform: Write your beacon interaction logic once in commonMain and deploy on both Android and iOS.
- Secure by Default: Uses KVault to provide encrypted storage for cryptographic keys and credentials on  both platforms (leveraging Android's EncryptedSharedPreferences and  iOS's Keychain).
- Flexible Configuration: A clean, DSL-based configuration allows you to easily adapt the SDK to  custom beacon hardware (UUIDs, Manufacturer IDs) and server environments (URLs, API paths).
- Robust Error Handling: All asynchronous I/O operations return a clear SdkResult, eliminating unexpected crashes and forcing developers to handle failures gracefully.
- Multiple API Layers:
  - High-Level Use Cases: Simple, one-shot classes (...Flow) for common operations like device registration and PoL transactions.
  - Low-Level Controllers: Direct access to core components like BleController and ApiClient for building custom logic.

## Requirements

- Android Studio: Narwhal (2025.1.1)
- Kotlin Version: 2.2.0+
- Android Target:
  - minSdk: 24 (Android 7.0)
  - compileSdk: 35

## Installation

Currently, the Polaris SDK is not published to a public repository like Maven  Central. To use it in your project, you must include it as a local  Gradle module.

1. Clone the Polaris SDK repository into your project's root directory or add it as a Git submodule.

2. In your application's `settings.gradle.kts` file, include the SDK module:

   ```
   include(":polaris-sdk")
   ```

   

In your application's `build.gradle.kts` file, add the project dependency:

```
dependencies {
	implementation(project(":polaris-sdk"))
    // ... other dependencies
}
```



## Getting started

Using the Polaris SDK involves two main steps: initializing it once when your app starts, and then using its components or high-level flows to  perform actions.

### 1. Initialization

The best place to initialize the SDK is in your Application's onCreate method to ensure it's ready before any UI is shown.

`Polaris.initialize()` is a suspend function, so it must be called from a coroutine. For simplicity in a sample app, you can use runBlocking.

```kotlin
      // In your app's Application class (e.g., PolarisApplication.kt)
import android.app.Application
import ch.drcookie.polaris_sdk.api.Polaris
import ch.drcookie.polaris_sdk.api.config.AuthMode
import kotlinx.coroutines.runBlocking

class PolarisApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize the SDK once when the app starts.
        runBlocking {
            Polaris.initialize(applicationContext) {
                // The configuration block is optional, but highly recommended.

                // Configure BLE settings if your beacons differ from the default.
                ble {
                    manufacturerId = 0x1234 // Example: Use a custom manufacturer ID
                }

                // Configure and enable the network client for server communication.
                // If this block is omitted, network features will be disabled.
                api {
                    baseUrl = "https://your.server.com"
                    // Choose your authentication mode:
                    authMode = AuthMode.ManagedApiKey // The SDK handles the key after registration.
                    // Or for a static key:
                    // authMode = AuthMode.StaticApiKey("your-static-api-key")
                    // Or for no auth:
                    // authMode = AuthMode.None
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Polaris.shutdown() // Clean up SDK resources
    }
}
```



### 2. Granting permissions (Android)

Your Android application must request the necessary Bluetooth and Location  permissions. The required permissions vary by Android version. A typical implementation in your Activity would handle this.

Required permissions in AndroidManifest.xml:

```xml
<!-- For Android 12 (API 31) and above -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<!-- For Android 10+ for location access during scans -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```



## API overview

The Polaris SDK offers two layers of API access through the Polaris singleton object.

### High-Level API: Use Cases (Flow classes)

These are pre-packaged classes that perform a complete end-to-end operation.  This is the recommended way to get started quickly. They are created by  your app (e.g., in a ViewModelFactory) and injected with dependencies  from Polaris.

- RegisterDeviceFlow(apiClient, keyStore): Registers the device with the backend and retrieves a list of known beacons.
- ScanForBeaconFlow(bleController, apiClient): Scans for a nearby connectable beacon from the list of known beacons.
- PolTransactionFlow(bleController, apiClient, keyStore, protocolHandler): Performs the full Proof-of-Location transaction with a found beacon.
- MonitorBroadcastsFlow(bleController, apiClient, protocolHandler): Starts a continuous scan to listen for and verify beacon broadcasts.
- DeliverPayloadFlow(bleController, apiClient, scanForBeacon):Finds a beacon and delivers a secure payload to it.

### Low-Level API: core components

For advanced use cases or custom logic, you can directly access the core components of the SDK.

- `BleController`
  - The main interface for all BLE interactions.
  - `connect()`, `disconnect()`, `requestPoL()`, `deliverSecurePayload()`
  - `findConnectableBeacons()`, `monitorBroadcasts()`
  - Exposes connectionState: `StateFlow<ConnectionState>`
- `ApiClient`
  - The main interface for all network interactions. Returns SdkResult for all operations.
  - `registerPhone()`, `submitPoLToken()`, `getPayloadsForDelivery()`, `submitSecureAck()`.
  - Exposes knownBeacons: `List<Beacon>` after a successful registration.
- `KeyStore`
  - Interface for secure cryptographic key storage.
  - `getOrCreateSignatureKeyPair()`, `clearAllKeys()`.
- `ProtocolHandler`
  - Interface for synchronous, low-level cryptographic protocol operations.
  - `signPoLRequest()`, `verifyPoLResponse()`, `verifyBroadcast()`.

## Examples

All operations that can fail return an SdkResult. You should handle this in your application code using a when block.

### Example 1: Registering a Device

This would typically be in your ViewModel.

```kotlin
class MyViewModel(private val apiClient: ApiClient) : ViewModel() {

	private val registerDevice = RegisterDeviceFlow(Polaris.apiClient, Polaris.keyStore)

    fun performRegistration() {
        viewModelScope.launch {
            val result = registerDevice(
                deviceModel = Build.MODEL,
                osVersion = Build.VERSION.RELEASE,
                appVersion = "1.0.0"
            )

            when (result) {
                is SdkResult.Success -> {
                    val beaconCount = result.value
                    log("Registration successful! Found $beaconCount beacons.")
                }
                is SdkResult.Failure -> {
                    log("Registration failed: ${result.error.message()}")
                }
            }
        }
    }
}   
```



### Example 2: Performing a Full PoL Token Flow

```kotlin
class MyViewModel(...) : ViewModel() {

    private val scanForBeacon = ScanForBeaconFlow(Polaris.bleController, Polaris.apiClient)
    private val performPolTransaction = PolTransactionFlow(
        Polaris.bleController,
        Polaris.apiClient,
        Polaris.keyStore,
        Polaris.protocolHandler
    )

    fun executeFullTokenFlow() {
        viewModelScope.launch {
            log("Scanning for a beacon...")
            val scanResult = scanForBeacon()

            val foundBeacon = when (scanResult) {
                is SdkResult.Success -> scanResult.value
                is SdkResult.Failure -> {
                    log("Scan failed: ${scanResult.error.message()}")
                    return@launch
                }
            }

            if (foundBeacon == null) {
                log("No beacon found in range.")
                return@launch
            }

            log("Beacon found! Performing PoL transaction...")
            val transactionResult = performPolTransaction(foundBeacon)

            when (transactionResult) {
                is SdkResult.Success -> {
                    val token = transactionResult.value
                    log("PoL Token created successfully! Submitting to server...")
                    
                    // Now submit the token
                    val submitResult = Polaris.apiClient.submitPoLToken(token)
                    if (submitResult is SdkResult.Failure) {
                        log("Failed to submit token: ${submitResult.error.message()}")
                    } else {
                        log("Token submitted!")
                    }
                }
                is SdkResult.Failure -> {
                    log("PoL transaction failed: ${transactionResult.error.message()}")
                }
            }
        }
    }
}
```



## What's Next? (Roadmap)

- Complete the iosMain implementation for all components.
- Add a comprehensive suite of unit and integration tests in commonTest.
- Publish the library to Maven Central for easy access.
- Add more detailed examples and tutorials to the documentation.

## Libraries & Licenses

The Polaris SDK uses several open-source libraries. Your application must comply with their licenses.

- [kotlin-logging 7.0.7](https://klibs.io/project/oshai/kotlin-logging): Apache 2.0
- [Ktor 3.1.3](https://klibs.io/project/ktorio/ktor): Apache 2.0
- [Kotlinx Serialization 1.8.1](https://klibs.io/project/Kotlin/kotlinx.serialization): Apache 2.0
- [Kotlinx atomicfu 0.29.0](https://klibs.io/project/Kotlin/kotlinx-atomicfu): Apache 2.0
- [KVault 1.12.0](https://klibs.io/project/Liftric/KVault): MIT
- [kotlin-multiplatform-libsodium 0.9.2](https://klibs.io/project/ionspin/kotlin-multiplatform-libsodium): Apache 2.0