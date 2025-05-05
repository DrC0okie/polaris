#include <Arduino.h>
#include <Preferences.h>  // for NVS

#include <memory>  // for std::unique_ptr

#include "ble/ble_server.h"
#include "protocol/crypto.h"
#include "protocol/pol_request_processor.h"
#include "utils/counter.h"
extern "C" {  // If crypto functions are C
#include "monocypher.h"
}

const uint32_t BEACON_ID = 1;
const char* NVS_NAMESPACE = "polaris-beacon";
const char* NVS_SECRET_KEY_NAME = "beacon_sk";

MinuteCounter counter(NVS_NAMESPACE, "counter");
BleServer server(PoLRequest::packedSize());

uint8_t sk[32];  // Beacon's secret key
uint8_t pk[32];  // Beacon's public key

bool loadOrGenerateKeys(Preferences& prefs, uint8_t* public_key,
                        uint8_t* secret_key);

void setup() {
    Serial.begin(115200);
    delay(6000);

    Serial.println("\n[MAIN] Booting Polaris Beacon...");

    // Initialize NVS
    Preferences preferences;
    if (!preferences.begin(NVS_NAMESPACE, false)) {  // 'false' for read/write
        Serial.println("[MAIN] Failed to initialize NVS!");
        delay(1000);
        ESP.restart();
    }

    Serial.println("[MAIN] NVS Initialized.");

    counter.begin();
    Serial.printf("[MAIN] Counter initialized. Current value: %u\n",
                  counter.getValue());

    // Load or Generate BLE Identity Keys
    if (loadOrGenerateKeys(preferences, pk, sk)) {
        Serial.println("[MAIN] Key pair loaded/generated successfully.");
        Serial.print("[MAIN] Beacon Public Key: ");
        for (int i = 0; i < sizeof(pk); ++i)
            Serial.printf("%02X", pk[i]);
        Serial.println();
    } else {
        Serial.println("[MAIN] CRITICAL: Failed to load or generate keys!");
        delay(1000);
        ESP.restart();
    }

    preferences.end();

    server.begin();

    // Get indication characteristic AFTER server.begin()
    auto* indicationChar = server.getIndicationCharacteristic();
    if (!indicationChar) {
        Serial.println("[MAIN] Could not get indication characteristic");
        delay(1000);
        ESP.restart();
    }

    // Create and Set the Request Processor
    PoLRequestProcessor* processor =
        new PoLRequestProcessor(BEACON_ID, sk, pk, counter, indicationChar);

    if (processor == nullptr) {
        ESP.restart();
    } else {
        server.setRequestProcessor(processor);
    }

    Serial.println("[MAIN] Setup complete. Beacon is operational.");
}

void loop() {
    // Prevent watchdog timeout if loop gets complex
    vTaskDelay(pdMS_TO_TICKS(1000));
}

// --- Key Management Function ---
bool loadOrGenerateKeys(Preferences& prefs, uint8_t* public_key,
                        uint8_t* secret_key) {
    Serial.println("[KEYS] Attempting to load secret key from NVS...");

    // Try to load the secret key
    size_t key_len = prefs.getBytesLength(NVS_SECRET_KEY_NAME);

    if (key_len == 32) {
        if (prefs.getBytes(NVS_SECRET_KEY_NAME, secret_key, 32) == 32) {
            Serial.println("[KEYS] Secret key loaded successfully from NVS.");
            // Derive the public key from the loaded secret key
            crypto_sign_public_key(public_key, secret_key);
            return true;
        } else {
            Serial.println("[KEYS] Error reading secret key from NVS despite "
                           "correct length reported.");
        }
    } else if (key_len == 0) {
        Serial.println("[KEYS] No secret key found in NVS.");
    } else {
        Serial.printf("[KEYS] Found key with incorrect length (%d bytes) in "
                      "NVS. Discarding.\n",
                      key_len);
    }

    Serial.println("[KEYS] Generating new Ed25519 key pair...");
    generateKeyPair(public_key,
                    secret_key);  // Assumes this fills both pk and sk
    Serial.println("[KEYS] New key pair generated.");

    Serial.println("[KEYS] Storing new secret key to NVS...");
    size_t bytes_written = prefs.putBytes(NVS_SECRET_KEY_NAME, secret_key, 32);

    if (bytes_written == 32) {
        Serial.println("[KEYS] New secret key stored successfully.");
        return true;
    } else {
        Serial.printf(
            "[KEYS] Failed to store new secret key! Wrote %d bytes.\n",
            bytes_written);
        return false;
    }
}