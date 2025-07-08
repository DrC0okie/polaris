# polaris-app

![alt text](https://img.shields.io/badge/language-Kotlin-7F52FF.svg)


![alt text](https://img.shields.io/badge/tech-Kotlin%20Multiplatform-blue.svg)

This repository contains the Polaris SDK, a Kotlin Multiplatform library designed to interact with the Polaris Proof-of-Location ecosystem, and a sample Android application demonstrating its usage.

The SDK provides a high-level and type-safe API for mobile applications to:

- Discover and monitor Polaris beacons.
- Perform secure Proof-of-Location (PoL) transactions.
- Act as a data relay for the asynchronous, end-to-end encrypted communication channel between beacons and the central server.

## SDK architecture & features

The Polaris SDK is built with Kotlin Multiplatform, enabling a single  codebase for core business logic, protocol handling, and network  communication, while seamlessly integrating with native platform  features.

- Built with Kotlin Coroutines and Flow, providing a non-blocking, structured concurrency model.
- All fallible operations return a `SdkResult` sealed class, forcing explicit handling of `Success` and `Failure` states and eliminating unexpected exceptions.
- Clean Architecture:
  -  A simple singleton entry point for easy initialization and access to all SDK components.
  - Pre-built flows (PolTransaction, DeliverPayload, etc.) that manage operations with a single call.
  - Access to lower-level controllers (BleController, NetworkClient) for developers who need more granular control.
- A Kotlin-based DSL allows for easy configuration of BLE parameters and network endpoints during initialization.
- Uses KVault for cross-platform, secure storage of cryptographic keys and API tokens.

### Project structure

The repository is a multi-project setup:

- <root>/polaris-sdk: The Kotlin Multiplatform SDK module. This is the core library you would publish and include in your own applications.
- <root>/app: A sample Android application that consumes and demonstrates the usage of the polaris-sdk module.

```
polaris-project
├── app/                  # Sample Android Application
└── polaris-sdk/          # The KMP SDK Library
    └── src/
        ├── commonMain/   # Core logic, interfaces, data models (shared)
        ├── androidMain/  # Android-specific implementations (BLE controller, etc.)
        └── iosMain/      # iOS-specific implementations (incomplete)
```



### Requirements

- Android Studio: Narwhal (2025.1.1)
- Kotlin Version: 2.2.0+

## Getting started: using the SDK

Currently, the Polaris SDK is not published to a public repository like Maven  Central. To use it in your project, you must include it as a local  Gradle module. In your application's `settings.gradle.kts` file, include the SDK module:

```
// app/build.gradle.kts
dependencies {
    implementation(project(":polaris-sdk"))
}
```



Your application's AndroidManifest.xml must include the necessary permissions for Bluetooth and Internet access.

```xml
<!-- Required for scanning and connecting on Android 12+ -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Required for location access during scanning on older Android versions -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>

<!-- Required for server communication -->
<uses-permission android:name="android.permission.INTERNET" />
```



The SDK must be initialized once before use, typically in your Application class. This must be done from a coroutine.

```kotlin
// In your custom Application class
import ch.drcookie.polaris_sdk.api.Polaris
import kotlinx.coroutines.runBlocking

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Use a proper CoroutineScope in a real app, e.g., using ProcessLifecycleOwner
        runBlocking {
            Polaris.initialize(applicationContext) {
                // Configure the network client
                network {
                    baseUrl = "https://your-polaris-server.com"
                    // Other optional settings...
                }
                // Configure BLE parameters (optional, defaults are provided)
                ble {
                    mtu = 256
                }
            }
        }
    }
}
```

Once initialized, you can access the SDK's components via the Polaris object. The easiest way to use the SDK is through its high-level use cases.



### API overview

The Polaris SDK offers two layers of API access through the Polaris singleton object.

#### High-level API: use cases

These are pre-packaged classes that perform a complete end-to-end operation.  This is the recommended way to get started quickly. They are created by  your app (e.g., in a ViewModelFactory) and injected with dependencies  from Polaris.

- `FetchBeacons(networkClient, keyStore)`: Registers the device with the backend and retrieves a list of known beacons.
- `ScanForBeacon(bleController, networkClient)`: Scans for a nearby connectable beacon from the list of known beacons.
- `PolTransaction(bleController, networkClient, keyStore, protocolHandler)`: Performs the full Proof-of-Location transaction with a found beacon.
- `MonitorBroadcasts(bleController, networkClient, protocolHandler)`: Starts a continuous scan to listen for and verify beacon broadcasts.
- `DeliverPayload(bleController, networkClient, scanForBeacon)`:Finds a beacon and delivers a secure payload to it.
- `PullAndForward(bleController, networkClient)`: handles the full beacon-to-server data use case.

#### Low-level API: core components

For advanced use cases or custom logic, you can directly access the core components of the SDK.

- `BleController`
  - The main interface for all BLE interactions.
  - `connect()`, `disconnect()`, `requestPoL()`, `deliverSecurePayload()`
  - `findConnectableBeacons()`, `monitorBroadcasts()`
  - Exposes connectionState: `StateFlow<ConnectionState>`
- `NetworkClient`
  - The main interface for all network interactions. Returns SdkResult for all operations.
  - `registerPhone()`, `submitPoLToken()`, `getPayloadsForDelivery()`, `submitSecureAck()`.
  - Exposes knownBeacons: `List<Beacon>` after a successful registration.
- `KeyStore`
  - Interface for secure cryptographic key storage.
  - `getOrCreateSignatureKeyPair()`, `clearAllKeys()`.
- `ProtocolHandler`
  - Interface for synchronous, low-level cryptographic protocol operations.
  - `signPoLRequest()`, `verifyPoLResponse()`, `verifyBroadcast()`.



### Examples

All operations that can fail return an SdkResult. You should handle this in your application code using a when block. This would typically be in your ViewModel.

#### Example 1: registering a device or fetching beacons

```kotlin
class PolarisViewModel(
    private val fetchBeacons: FetchBeacons
) : ViewModel() {

        fun fetchBeacons() {
        runFlow("Fetch beacons") {
            appendLog("Registering phone with server...")

            when (val result = fetchBeacons(Build.MODEL, Build.VERSION.RELEASE, "1.0")) {
                is SdkResult.Success -> {
                    val beaconCount = result.value
                    appendLog("Fetch success, found $beaconCount known beacons.")
                }

                is SdkResult.Failure -> {
                    appendLog("--- ERROR: Fetch failed: ${result.error.message()} ---")
                }
            }
        }
    }
}   
```



#### Example 2: performing a poL token flow

```kotlin
class ViewModel(
    private val scanForBeacon: ScanForBeacon,
    private val performPolTransaction: PolTransaction,
) : ViewModel() {

    fun findAndExecuteTokenFlow() {
        runFlow("PoL Token Flow") {
            if (networkClient.knownBeacons.isEmpty()) {
                appendLog("No known beacons. Please register first.")
                return@runFlow
            }

            appendLog("Scanning for first known beacon...")

            // Check the scan result
            val foundBeacon = when (val scanResult = scanForBeacon()) {
                is SdkResult.Success -> scanResult.value
                is SdkResult.Failure -> {
                    appendLog("--- ERROR: Scan failed: ${scanResult.error.message()} ---")
                    return@runFlow
                }
            }

            // Check if a beacon was actually found (vs. timeout)
            if (foundBeacon == null) {
                appendLog("Scan timed out. No known beacons found.")
                return@runFlow
            }
            appendLog("Found beacon: ${foundBeacon.name}. Performing transaction...")

            // PoL transaction
            val token = when (val transactionResult = performPolTransaction(foundBeacon)) {
                is SdkResult.Success -> transactionResult.value
                is SdkResult.Failure -> {
                    appendLog("--- ERROR: PoL Transaction failed: ${transactionResult.error.message()} ---")
                    return@runFlow
                }
            }
            appendLog("PoL transaction successful. Submitting token...")

            // Submit the token to the server
            when (val submitResult = networkClient.submitPoLToken(token)) {
                is SdkResult.Success -> {
                    appendLog("Token submitted successfully!")
                }

                is SdkResult.Failure -> {
                    appendLog("--- ERROR: Failed to submit token: ${submitResult.error.message()} ---")
                }
            }
        }
    }
}
```



The sample application shows more usage examples.



## Running the sample application

The app module provides a comprehensive example of how to use the SDK.

1. Ensure the Polaris server is running and accessible.
2. In app/src/main/java/.../PolarisViewModelFactory.kt, update the baseUrl in the Polaris.initialize block to point to your server instance.
3. Open the project in Android Studio, select the app configuration, and run it on an emulator or a physical device.

The sample app demonstrates:

- Requesting required permissions at runtime.
- Initializing the SDK in a ViewModelProvider.Factory.
- Using a ViewModel and StateFlow to manage UI state.
- Executing all major SDK use cases (registration, PoL, payload delivery, etc.).
- Displaying a running log of SDK operations and results.



## What's next? (Roadmap)

- Complete the iosMain implementation for all components.
- Add a comprehensive suite of unit and integration tests in commonTest.
- Publish the library to Maven Central for easy access.
- Add more detailed examples and tutorials to the documentation.

## Libraries & licenses

The Polaris SDK uses several open-source libraries. Your application must comply with their licenses.

- [kotlin-logging 7.0.7](https://klibs.io/project/oshai/kotlin-logging): Apache 2.0
- [Ktor 3.1.3](https://klibs.io/project/ktorio/ktor): Apache 2.0
- [Kotlinx Serialization 1.8.1](https://klibs.io/project/Kotlin/kotlinx.serialization): Apache 2.0
- [Kotlinx atomicfu 0.29.0](https://klibs.io/project/Kotlin/kotlinx-atomicfu): Apache 2.0
- [KVault 1.12.0](https://klibs.io/project/Liftric/KVault): MIT
- [kotlin-multiplatform-libsodium 0.9.2](https://klibs.io/project/ionspin/kotlin-multiplatform-libsodium): Apache 2.0