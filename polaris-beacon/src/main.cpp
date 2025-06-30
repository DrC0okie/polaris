#include <Arduino.h>
#include <preferences.h>
#include <sodium.h>

#include "ble/broadcast_advertiser.h"
#include "ble/ble_manager.h"
#include "ble/connectable_advertiser.h"
#include "protocol/handlers/commands/command_factory.h"
#include "protocol/handlers/data_pull_handler.h"
#include "protocol/handlers/encrypted_message_handler.h"
#include "protocol/handlers/outgoing_message_service.h"
#include "protocol/handlers/token_message_handler.h"
#include "utils/beacon_counter.h"
#include "utils/crypto_service.h"
#include "utils/display_controller.h"
#include "utils/key_manager.h"
#include "utils/led_controller.h"
#include "utils/system_monitor.h"

// These objects are declared globally to ensure their lifecycle persists for
// the entire duration of the program.
const char* TAG = "[MAIN]";
BleManager ble;
Preferences prefs;
BeaconCounter counter;
KeyManager keyManager;
CryptoService cryptoService(keyManager);
LedController ledController;
DisplayController displayController;
SystemMonitor systemMonitor;
OutgoingMessageService outgoingMessageService;
CommandFactory commandFactory(ledController, displayController, systemMonitor,
                              outgoingMessageService);
std::unique_ptr<BroadcastAdvertiser> beaconExtAdvertiser;
std::unique_ptr<ConnectableAdvertiser> connectableAdvertiser;
std::vector<std::unique_ptr<FragmentationTransport>> g_transports;
std::vector<std::unique_ptr<IMessageHandler>> g_handlers;
FragmentationTransport* encryptedTransportPtr = nullptr;

void setup() {
    Serial.begin(115200);
    delay(5000);  // Gives the developper time to connect the serial monitor
    Serial.printf("\n%s Booting Polaris Beacon...\n", TAG);

    ledController.begin();
    if (!displayController.begin()) {
        Serial.println("DisplayController error!");
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

    // Create the advertiser for the connectable (legacy) advertisement.
    connectableAdvertiser =
        std::unique_ptr<ConnectableAdvertiser>(new ConnectableAdvertiser(*multiAdv));
    connectableAdvertiser->begin();

    // Initialize the outgoing message service. It needs a callback to notify the
    // connectable advertiser when its queue state changes.
    outgoingMessageService.begin(&cryptoService, &prefs, [&](bool hasData) {
        connectableAdvertiser->setHasDataPending(hasData);
    });

    // Create the advertiser for the non-connectable (extended) broadcast.
    beaconExtAdvertiser = std::unique_ptr<BroadcastAdvertiser>(
        new BroadcastAdvertiser(BEACON_ID, cryptoService, counter, *multiAdv));
    beaconExtAdvertiser->begin();

    // Get the raw BLE characteristic that will be used for sending data.
    auto tokenIndicateChar = ble.getCharacteristicByUUID(BLEUUID(BleManager::TOKEN_INDICATE));

    // Create the Transport Layer for this channel.
    auto tokenTransport = std::unique_ptr<FragmentationTransport>(new FragmentationTransport(
        tokenIndicateChar,  // The characteristic to use for sending data OUT.
        // Provide a Factory Lambda to create the Message Handler.
        [&](IMessageTransport& transport) -> std::unique_ptr<IMessageHandler> {
            // Inside the lambda, create the TokenMessageHandler for this channel.
            return std::unique_ptr<TokenMessageHandler>(
                new TokenMessageHandler(cryptoService, counter, transport));
        }));

    ble.setTokenRequestProcessor(tokenTransport.get());
    ble.registerTransportForMtuUpdates(tokenTransport.get());
    g_transports.push_back(std::move(tokenTransport));

    // Setup for all encrypted communication.
    auto encIndicateChar = ble.getCharacteristicByUUID(BLEUUID(BleManager::ENCRYPTED_INDICATE));
    auto encryptedTransport = std::unique_ptr<FragmentationTransport>(new FragmentationTransport(
        encIndicateChar, [&](IMessageTransport& transport) -> std::unique_ptr<IMessageHandler> {
            return std::unique_ptr<EncryptedMessageHandler>(new EncryptedMessageHandler(
                cryptoService, counter, prefs, transport, commandFactory, outgoingMessageService));
        }));

    ble.setEncryptedDataProcessor(encryptedTransport.get());
    encryptedTransportPtr = encryptedTransport.get();
    ble.registerTransportForMtuUpdates(encryptedTransport.get());
    g_transports.push_back(std::move(encryptedTransport));

    // Setup the handler for the explicit "data pull" trigger.
    auto dataPullHandler = std::unique_ptr<DataPullHandler>(
        new DataPullHandler(outgoingMessageService, *encryptedTransportPtr));
    ble.setPullRequestProcessor(dataPullHandler.get());
    g_handlers.push_back(std::move(dataPullHandler));

    Serial.printf("%s Setup complete. Beacon is operational.\n", TAG);
}

void loop() {
    vTaskDelay(pdMS_TO_TICKS(1000));
}
