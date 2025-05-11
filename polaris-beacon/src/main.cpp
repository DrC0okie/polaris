#include <Arduino.h>
#include <Preferences.h>
#include <sodium.h>

#include "ble/beacon_advertiser.h"
#include "ble/ble_server.h"
#include "protocol/crypto.h"
#include "protocol/encrypted_data_processor.h"
#include "protocol/pol_request_processor.h"
#include "utils/counter.h"
#include "utils/key_storage.h"

// Global
const char* TAG = "[MAIN]";

BleServer server;

uint8_t ed25519_sk[POL_Ed25519_SK_SIZE];
uint8_t ed25519_pk[POL_Ed25519_PK_SIZE];
uint8_t x25519_sk[POL_X25519_SK_SIZE];
uint8_t x25519_pk[POL_X25519_PK_SIZE];
uint8_t server_x25519_pk[POL_X25519_PK_SIZE];

// Global BeaconAdvertiser
std::unique_ptr<BeaconAdvertiser> beaconExtAdvertiser;

void setup() {
    Serial.begin(115200);
    delay(5000);
    Serial.printf("%s Booting Polaris Beacon...\n", TAG);

    if (sodium_init() == -1) {
        Serial.printf("%s CRITICAL: Libsodium initialization failed! Restarting...\n", TAG);
        ESP.restart();
    }
    Serial.printf("%s Libsodium initialized.\n", TAG);

    Preferences prefs;
    if (!prefs.begin(NVS_NAMESPACE, false)) {
        Serial.printf("%s CRITICAL: Failed to initialize NVS! Restarting...\n", TAG);
        ESP.restart();
    }
    Serial.printf("%s NVS Initialized.\n", TAG);

    MinuteCounter counter(prefs, "counter");
    Serial.printf("TAG Counter initialized. Current value: %llu\n", TAG, counter.getValue());

    KeyStorage keyManager(prefs);

    // Manage Ed25519 keys using KeyStorage
    if (!keyManager.manageEd25519KeyPair(ed25519_pk, ed25519_sk)) {
        Serial.printf("%s CRITICAL: Failed to manage Ed25519 keys! Restarting...\n", TAG);
        ESP.restart();
    }

    Serial.printf("%s Beacon Ed25519 Public Key: ", TAG);
    for (int i = 0; i < POL_Ed25519_PK_SIZE; ++i)
        Serial.printf("%02X", ed25519_pk[i]);
    Serial.println();

    // Manage X25519 keys using KeyStorage
    if (!keyManager.manageX25519KeyPair(x25519_pk, x25519_sk)) {
        Serial.printf("%s CRITICAL: Failed to manage X25519 keys! Restarting...\n", TAG);
        ESP.restart();
    }

    Serial.printf("%s X25519 Public Key: ", TAG);
    for (int i = 0; i < POL_X25519_PK_SIZE; ++i)
        Serial.printf("%02X", x25519_pk[i]);
    Serial.println();

    // Manage Server's X25519 Public Key using KeyStorage
    if (!keyManager.manageServerX25519PublicKey(server_x25519_pk, HARDCODED_SERVER_X25519_PK)) {
        Serial.printf("%s CRITICAL: Failed to manage Server's X25519 PK! Restarting...\n", TAG);
        ESP.restart();
    }
    Serial.printf("%s Server X25519 Public Key: ", TAG);
    for (int i = 0; i < POL_X25519_PK_SIZE; ++i)
        Serial.printf("%02X", server_x25519_pk[i]);
    Serial.println();

    prefs.end();

    Serial.printf("%s Starting GATT Server & Multi-Advertising...\n", TAG);
    server.begin(BLE_DEVICE_NAME);  // This will internally use g_advertiser

    BLEMultiAdvertising* actual_advertiser_ptr = server.getMultiAdvertiser();
    if (!actual_advertiser_ptr) {
        Serial.printf("%s CRITICAL: Could not get multi-advertiser from BleServer! Halting.\n",
                      TAG);
        while (1) {
            delay(1000);
        }
    }

    beaconExtAdvertiser = std::unique_ptr<BeaconAdvertiser>(
        new BeaconAdvertiser(BEACON_ID, ed25519_sk, counter, *actual_advertiser_ptr));

    beaconExtAdvertiser->begin();  // This will set the initial extended adv data

    // --- Setup token Request Processor ---
    BLECharacteristic* tokenIndChar =
        server.getCharacteristicByUUID(BLEUUID(BleServer::TOKEN_INDICATE));
    if (!tokenIndChar) {
        Serial.printf(" CRITICAL: token Indication characteristic is null Restarting...\n", TAG);
        ESP.restart();
    }

    auto tokenProcessor = std::unique_ptr<PoLRequestProcessor>(
        new PoLRequestProcessor(BEACON_ID, ed25519_sk, counter, tokenIndChar));

    if (!tokenProcessor) {
        Serial.printf("%s CRITICAL: Failed to allocate token processor! Restarting...\n", TAG);
        ESP.restart();
    }
    server.setTokenRequestProcessor(std::move(tokenProcessor));

    BLECharacteristic* encIndChar =
        server.getCharacteristicByUUID(BLEUUID(BleServer::ENCRYPTED_INDICATE));
    if (!encIndChar) {
        Serial.printf(
            "%s CRITICAL: Encrypted Data indication characteristic is null! Restarting...\n", TAG);
        ESP.restart();
    }

    auto encryptedProcessor =
        std::unique_ptr<EncryptedDataProcessor>(new EncryptedDataProcessor(encIndChar));
    if (!encryptedProcessor) {
        Serial.printf("%s CRITICAL: Failed to allocate EncryptedDataProcessor! Restarting...\n",
                      TAG);
        ESP.restart();
    }
    server.setEncryptedDataProcessor(std::move(encryptedProcessor));

    Serial.printf("%s Setup complete. Beacon is operational.\n", TAG);
}

void loop() {
    vTaskDelay(pdMS_TO_TICKS(1000));
}
