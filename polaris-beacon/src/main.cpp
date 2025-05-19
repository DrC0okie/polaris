#include <Arduino.h>
#include <preferences.h>
#include <sodium.h>

#include "ble/beacon_advertiser.h"
#include "ble/ble_server.h"
#include "protocol/crypto.h"
#include "protocol/handlers/encrypted_message_handler.h"
#include "protocol/handlers/token_message_handler.h"
#include "utils/counter.h"
#include "utils/key_manager.h"
#include "utils/utils.h"

// Globals that need to have infinite lifecycle
const char* TAG = "[MAIN]";
BleServer server;
Preferences prefs;
MinuteCounter counter;
KeyManager keyManager;
std::unique_ptr<BeaconAdvertiser> beaconExtAdvertiser;

void setup() {
    Serial.begin(115200);
    delay(5000);
    Serial.printf("\n%s Booting Polaris Beacon...\n", TAG);

    // Init crypto lib
    if (sodium_init() == -1) {
        Serial.printf("%s CRITICAL: Libsodium initialization failed! Restarting...\n", TAG);
        ESP.restart();
    }

    // Init the non-volatile storage (NVS)
    if (!prefs.begin(NVS_NAMESPACE, false)) {
        Serial.printf("%s CRITICAL: Failed to initialize NVS! Restarting...\n", TAG);
        ESP.restart();
    }
    Serial.printf("%s NVS Initialized.\n", TAG);
    // WARNING: Do not close the NVS namespace, we need it opened for the minute counter and message
    // ids

    counter.begin(prefs);
    if (!keyManager.begin(prefs)) {
        Serial.printf("%s CRITICAL: Failed to initialize Key manager, rebooting...\n", TAG);
        ESP.restart();
    }

    Serial.printf("%s Starting GATT Server & Multi-Advertising...\n", TAG);
    server.begin(BLE_DEVICE_NAME);

    BLEMultiAdvertising* multiAdv = server.getMultiAdvertiser();
    if (!multiAdv) {
        Serial.printf("%s CRITICAL: Failed to get MultiAdvertiser! Restarting...\n", TAG);
        ESP.restart();
    }

    // create an advertizer to broadcast signed data
    beaconExtAdvertiser = std::unique_ptr<BeaconAdvertiser>(
        new BeaconAdvertiser(BEACON_ID, keyManager.getEd25519Sk(), counter, *multiAdv));

    beaconExtAdvertiser->begin();

    // Assign message handler to manage incoming Pol requests
    auto tokenProcessor = std::unique_ptr<TokenMessageHandler>(new TokenMessageHandler(
        BEACON_ID, keyManager.getEd25519Sk(), counter,
        server.getCharacteristicByUUID(BLEUUID(BleServer::TOKEN_INDICATE))));

    if (!tokenProcessor) {
        Serial.printf("%s CRITICAL: Failed to allocate token processor! Restarting...\n", TAG);
        ESP.restart();
    }
    server.setTokenRequestProcessor(std::move(tokenProcessor));

    // Assign message handler for encrypted payloads from server (through the phones)
    auto indication = server.getCharacteristicByUUID(BLEUUID(BleServer::ENCRYPTED_INDICATE));
    auto encryptedProcessor = std::unique_ptr<EncryptedMessageHandler>(
        new EncryptedMessageHandler(keyManager, counter, prefs, indication));
    if (!encryptedProcessor) {
        Serial.printf("%s CRITICAL: Failed to allocate EncryptedMessageHandler! Restarting...\n",
                      TAG);
        ESP.restart();
    }
    server.setEncryptedDataProcessor(std::move(encryptedProcessor));

    Serial.printf("%s Setup complete. Beacon is operational.\n", TAG);
}

void loop() {
    vTaskDelay(pdMS_TO_TICKS(1000));
}
