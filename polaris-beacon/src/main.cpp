#include <Arduino.h>
#include <preferences.h>
#include <sodium.h>

#include "ble/beacon_advertiser.h"
#include "ble/ble_manager.h"
#include "protocol/handlers/commands/command_factory.h"
#include "protocol/handlers/encrypted_message_handler.h"
#include "protocol/handlers/token_message_handler.h"
#include "utils/beacon_counter.h"
#include "utils/crypto_service.h"
#include "utils/display_controller.h"
#include "utils/key_manager.h"
#include "utils/led_controller.h"

// Globals that need to have infinite lifecycle
const char* TAG = "[MAIN]";
BleManager ble;
Preferences prefs;
BeaconCounter counter;
KeyManager keyManager;
CryptoService cryptoService(keyManager);
LedController ledController;
DisplayController displayController;
CommandFactory commandFactory(ledController, displayController);
std::unique_ptr<BeaconAdvertiser> beaconExtAdvertiser;
std::vector<std::unique_ptr<FragmentationTransport>> g_transports;

void setup() {
    Serial.begin(115200);
    delay(5000);
    Serial.printf("\n%s Booting Polaris Beacon...\n", TAG);

    ledController.begin();
    if (!displayController.begin()) {
        Serial.println("DisplayController error!");
        delay(50000);
    }

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
    // WARNING: Do not close the NVS namespace, we need it opened for the counter and messageId

    counter.begin(prefs);
    if (!keyManager.begin(prefs)) {
        Serial.printf("%s CRITICAL: Failed to initialize Key manager, rebooting...\n", TAG);
        ESP.restart();
    }

    Serial.printf("%s Starting GATT Server & Multi-Advertising...\n", TAG);
    ble.begin(BLE_DEVICE_NAME);

    BLEMultiAdvertising* multiAdv = ble.getMultiAdvertiser();
    if (!multiAdv) {
        Serial.printf("%s CRITICAL: Failed to get MultiAdvertiser! Restarting...\n", TAG);
        ESP.restart();
    }

    // create an advertizer to broadcast signed data
    beaconExtAdvertiser = std::unique_ptr<BeaconAdvertiser>(
        new BeaconAdvertiser(BEACON_ID, cryptoService, counter, *multiAdv));

    beaconExtAdvertiser->begin();

    auto tokenIndicateChar = ble.getCharacteristicByUUID(BLEUUID(BleManager::TOKEN_INDICATE));
    auto tokenTransport = std::unique_ptr<FragmentationTransport>(new FragmentationTransport(
        tokenIndicateChar, [&](IMessageTransport& transport) -> std::unique_ptr<IMessageHandler> {
            return std::unique_ptr<TokenMessageHandler>(
                new TokenMessageHandler(cryptoService, counter, transport));
        }));

    // Register the transport with the ble for processing incoming data and for MTU updates.
    ble.setTokenRequestProcessor(tokenTransport.get());
    ble.registerTransportForMtuUpdates(tokenTransport.get());
    g_transports.push_back(std::move(tokenTransport));

    // Setup Encrypted Message Handling
    auto encryptedIndicateChar =
        ble.getCharacteristicByUUID(BLEUUID(BleManager::ENCRYPTED_INDICATE));
    auto encryptedTransport = std::unique_ptr<FragmentationTransport>(new FragmentationTransport(
        encryptedIndicateChar,
        [&](IMessageTransport& transport) -> std::unique_ptr<IMessageHandler> {
            return std::unique_ptr<EncryptedMessageHandler>(new EncryptedMessageHandler(
                cryptoService, counter, prefs, transport, commandFactory));
        }));

    ble.setEncryptedDataProcessor(encryptedTransport.get());
    ble.registerTransportForMtuUpdates(encryptedTransport.get());
    g_transports.push_back(std::move(encryptedTransport));
    Serial.printf("%s Setup complete. Beacon is operational.\n", TAG);
}

void loop() {
    vTaskDelay(pdMS_TO_TICKS(1000));
}
