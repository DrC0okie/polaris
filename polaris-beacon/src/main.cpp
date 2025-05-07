#include <Arduino.h>
#include <Preferences.h>

#include "ble/ble_server.h"  // Includes BLEMultiAdvertising if 5.0 features are on
#include "protocol/crypto.h"
#include "protocol/pol_request_processor.h"
#include "ble/beacon_advertiser.h"
#include "utils/counter.h"
extern "C" {
#include "monocypher.h"
}

const uint32_t BEACON_ID = 1;
const char* NVS_NAMESPACE = "polaris-beacon";
const char* NVS_SECRET_KEY_NAME = "beacon_sk";
const char* BLE_DEVICE_NAME = "PoL Beacon";  // Example device name

// Global
MinuteCounter counter(NVS_NAMESPACE, "counter");
// BLEMultiAdvertising g_advertiser(NUM_ADV_INSTANCES);  // Global advertiser instance
BleServer server(PoLRequest::packedSize());

uint8_t sk[32];
uint8_t pk[32];

// Global BeaconAdvertiser
std::unique_ptr<BeaconAdvertiser> beaconExtAdvertiser;

bool loadOrGenerateKeys(Preferences& prefs, uint8_t* public_key, uint8_t* secret_key);

void setup() {
    Serial.begin(115200);
    delay(5000);
    Serial.println("\n[MAIN] Booting Polaris Beacon...");

    Preferences preferences;
    if (!preferences.begin(NVS_NAMESPACE, false)) {
        Serial.println("[MAIN] CRITICAL: Failed to initialize NVS! Restarting...");
        delay(1000);
        ESP.restart();
    }
    Serial.println("[MAIN] NVS Initialized.");

    counter.begin();
    Serial.printf("[MAIN] Counter initialized. Current value: %u\n", counter.getValue());

    if (loadOrGenerateKeys(preferences, pk, sk)) {
        Serial.println("[MAIN] Key pair loaded/generated successfully.");
        Serial.print("[MAIN] Beacon Public Key: ");
        for (int i = 0; i < 32; ++i)
            Serial.printf("%02X", pk[i]);
        Serial.println();
    } else {
        Serial.println("[MAIN] CRITICAL: Failed to load or generate keys! Restarting...");
        delay(1000);
        ESP.restart();
    }
    preferences.end();

    Serial.println("[MAIN] Starting GATT Server & Multi-Advertising...");
    server.begin(BLE_DEVICE_NAME);  // This will internally use g_advertiser

    BLEMultiAdvertising* actual_advertiser_ptr = server.getMultiAdvertiser();
    if (!actual_advertiser_ptr) {
        Serial.println("[MAIN] CRITICAL: Could not get multi-advertiser from BleServer! Halting.");
        while(1) { delay(1000); }
    }

    beaconExtAdvertiser = std::unique_ptr<BeaconAdvertiser>(
        new BeaconAdvertiser(BEACON_ID, sk, pk, counter, *actual_advertiser_ptr)
    );

    beaconExtAdvertiser->begin(); // This will set the initial extended adv data

    BLECharacteristic* indChar = server.getIndicationCharacteristic();
    if (!indChar) {
        Serial.println("[MAIN] CRITICAL: Indication characteristic is null "
                       "after server.begin()! Restarting...");
        delay(1000);
        ESP.restart();
    }

    auto processor = std::unique_ptr<PoLRequestProcessor>(
        new PoLRequestProcessor(BEACON_ID, sk, pk, counter, indChar));

    if (!processor) {
        Serial.println("[MAIN] CRITICAL: Failed to allocate "
                       "PoLRequestProcessor! Restarting...");
        delay(1000);
        ESP.restart();
    }
    server.setRequestProcessor(std::move(processor));

    Serial.println("[MAIN] Setup complete. Beacon is operational.");
}

void loop() {
    vTaskDelay(pdMS_TO_TICKS(1000));  // Main loop can be idle or do other non-BLE tasks
}

bool loadOrGenerateKeys(Preferences& prefs, uint8_t* public_key, uint8_t* secret_key) {
    Serial.println("[KEYS] Attempting to load secret key from NVS...");
    size_t key_len = prefs.getBytesLength(NVS_SECRET_KEY_NAME);

    if (key_len == 32) {
        if (prefs.getBytes(NVS_SECRET_KEY_NAME, secret_key, 32) == 32) {
            Serial.println("[KEYS] Secret key loaded successfully from NVS.");
            crypto_sign_public_key(public_key,
                                   secret_key);  // Derive public from secret
            return true;
        }
        Serial.println("[KEYS] Error reading secret key from NVS despite "
                       "correct length reported.");
    } else if (key_len == 0) {
        Serial.println("[KEYS] No secret key found in NVS.");
    } else {
        Serial.printf("[KEYS] Found key with incorrect length (%zu bytes) in "
                      "NVS. Discarding.\n",
                      key_len);
    }

    Serial.println("[KEYS] Generating new Ed25519 key pair...");
    generateKeyPair(public_key, secret_key);  // Assumes this populates both
    Serial.println("[KEYS] New key pair generated.");

    Serial.println("[KEYS] Storing new secret key to NVS...");
    if (prefs.putBytes(NVS_SECRET_KEY_NAME, secret_key, 32) == 32) {
        Serial.println("[KEYS] New secret key stored successfully.");
        return true;
    }
    Serial.println("[KEYS] Failed to store new secret key!");
    return false;
}