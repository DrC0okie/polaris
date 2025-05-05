#include <Arduino.h>
#include <BLEDevice.h>
#include <Preferences.h>  // for NVS

#include "ble/beacon_broadcaster.h"
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
const uint32_t BROADCAST_INTERVAL_MS = 5000;
const char* BLE_DEVICE_NAME = "PoL Beacon";

// Global Objects
MinuteCounter counter(NVS_NAMESPACE, "counter");
BleServer server(PoLRequest::packedSize());
BeaconBroadcaster broadcaster(counter);

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
    if (!preferences.begin(NVS_NAMESPACE, false)) {
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

    // Close NVS handle
    preferences.end();

    // --- Initialize BLE Stack FIRST ---
    Serial.println("[MAIN] Initializing BLE Stack...");
    BLEDevice::init(BLE_DEVICE_NAME);
    Serial.println("[MAIN] Waiting for BLE stack stabilization...");
    delay(500);  // Keep a moderate delay
    Serial.println("[MAIN] BLE Stack Initialized (post-delay).");

    // --- Initialize and Start Broadcaster (Simplified Ext Adv ONLY) ---
    Serial.println("[MAIN] Starting Extended Advertising Broadcaster...");
    if (!broadcaster.begin(BEACON_ID, sk, pk, BROADCAST_INTERVAL_MS)) {
        Serial.println("[MAIN] CRITICAL: Failed to start broadcaster! Check "
                       "logs. Halting...");
        while (1) {
            delay(1000);
        }
    } else {
        Serial.println(
            "[MAIN] Broadcaster begin() sequence initiated successfully.");
        // NOTE: Success here only means commands were issued without immediate
        // error. Asynchronous failures might still occur (check logs).
    }

    /*
    // --- Initialize GATT Server (AFTER broadcaster) --- // <-- UNCOMMENTED
    Serial.println("[MAIN] Starting GATT Server...");
    server.begin();  // Should NOT call BLEDevice::init()
    auto* indicationChar = server.getIndicationCharacteristic();
    if (!indicationChar) {
        Serial.println("[MAIN] CRITICAL: Could not get GATT indication "
                       "characteristic! Restarting...");
        //broadcaster.stop();   // Stop broadcaster before restart
        BLEDevice::deinit();  // Deinit BLE
        delay(1000);
        ESP.restart();
    }
    PoLRequestProcessor* processor =
        new PoLRequestProcessor(BEACON_ID, sk, pk, counter, indicationChar);
    if (processor == nullptr) {
        Serial.println("[MAIN] CRITICAL: Failed to allocate "
                       "PoLRequestProcessor! Restarting...");
        //broadcaster.stop();   // Stop broadcaster before restart
        BLEDevice::deinit();  // Deinit BLE
        delay(1000);
        ESP.restart();
    }
    server.setRequestProcessor(processor);
    Serial.println("[MAIN] GATT Server started and Processor configured.");
*/
    Serial.println("[MAIN] Setup complete. Beacon is operational.");
}

void loop() {
    vTaskDelay(pdMS_TO_TICKS(1000));
}

// Key Management Function
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

// #ifndef CONFIG_BT_BLE_50_FEATURES_SUPPORTED
// #error "This SoC does not support BLE5. Try using ESP32-C3, or ESP32-S3"
// #else

// #include <Arduino.h>
// #include <BLEAdvertising.h>
// #include <BLEDevice.h>

// esp_ble_gap_ext_adv_params_t ext_adv_params_1M = {
//     .type = ESP_BLE_GAP_SET_EXT_ADV_PROP_CONNECTABLE,
//     .interval_min = 0x30,
//     .interval_max = 0x30,
//     .channel_map = ADV_CHNL_ALL,
//     .own_addr_type = BLE_ADDR_TYPE_RANDOM,
//     .peer_addr_type = BLE_ADDR_TYPE_RANDOM,
//     .peer_addr = {0, 0, 0, 0, 0, 0},
//     .filter_policy = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY,
//     .tx_power = EXT_ADV_TX_PWR_NO_PREFERENCE,
//     .primary_phy = ESP_BLE_GAP_PHY_CODED,
//     .max_skip = 0,
//     .secondary_phy = ESP_BLE_GAP_PHY_1M,
//     .sid = 0,
//     .scan_req_notif = false,
// };

// esp_ble_gap_ext_adv_params_t ext_adv_params_2M = {
//     .type = ESP_BLE_GAP_SET_EXT_ADV_PROP_SCANNABLE,
//     .interval_min = 0x40,
//     .interval_max = 0x40,
//     .channel_map = ADV_CHNL_ALL,
//     .own_addr_type = BLE_ADDR_TYPE_RANDOM,
//     .peer_addr_type = BLE_ADDR_TYPE_RANDOM,
//     .peer_addr = {0, 0, 0, 0, 0, 0},
//     .filter_policy = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY,
//     .tx_power = EXT_ADV_TX_PWR_NO_PREFERENCE,
//     .primary_phy = ESP_BLE_GAP_PHY_1M,
//     .max_skip = 0,
//     .secondary_phy = ESP_BLE_GAP_PHY_2M,
//     .sid = 1,
//     .scan_req_notif = false,
// };

// esp_ble_gap_ext_adv_params_t legacy_adv_params = {
//     .type = ESP_BLE_GAP_SET_EXT_ADV_PROP_LEGACY_IND,
//     .interval_min = 0x45,
//     .interval_max = 0x45,
//     .channel_map = ADV_CHNL_ALL,
//     .own_addr_type = BLE_ADDR_TYPE_RANDOM,
//     .peer_addr_type = BLE_ADDR_TYPE_RANDOM,
//     .peer_addr = {0, 0, 0, 0, 0, 0},
//     .filter_policy = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY,
//     .tx_power = EXT_ADV_TX_PWR_NO_PREFERENCE,
//     .primary_phy = ESP_BLE_GAP_PHY_1M,
//     .max_skip = 0,
//     .secondary_phy = ESP_BLE_GAP_PHY_1M,
//     .sid = 2,
//     .scan_req_notif = false,
// };

// esp_ble_gap_ext_adv_params_t ext_adv_params_coded = {
//     .type = ESP_BLE_GAP_SET_EXT_ADV_PROP_SCANNABLE,
//     .interval_min = 0x50,
//     .interval_max = 0x50,
//     .channel_map = ADV_CHNL_ALL,
//     .own_addr_type = BLE_ADDR_TYPE_RANDOM,
//     .peer_addr_type = BLE_ADDR_TYPE_RANDOM,
//     .peer_addr = {0, 0, 0, 0, 0, 0},
//     .filter_policy = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY,
//     .tx_power = EXT_ADV_TX_PWR_NO_PREFERENCE,
//     .primary_phy = ESP_BLE_GAP_PHY_1M,
//     .max_skip = 0,
//     .secondary_phy = ESP_BLE_GAP_PHY_CODED,
//     .sid = 3,
//     .scan_req_notif = false,
// };

// static uint8_t raw_adv_data_1m[] = {
//     0x02, 0x01, 0x06, 0x02, 0x0a, 0xeb, 0x12, 0x09, 'E', 'S', 'P', '_', 'M',
//     'U',  'L',  'T',  'I',  '_',  'A',  'D',  'V',  '_', '1', 'M', 0X0};

// static uint8_t raw_scan_rsp_data_2m[] = {
//     0x02, 0x01, 0x06, 0x02, 0x0a, 0xeb, 0x12, 0x09, 'E', 'S', 'P', '_', 'M',
//     'U',  'L',  'T',  'I',  '_',  'A',  'D',  'V',  '_', '2', 'M', 0X0};

// static uint8_t legacy_adv_data[] = {0x02, 0x01, 0x06, 0x02, 0x0a, 0xeb, 0x15,
//                                     0x09, 'E',  'S',  'P',  '_',  'M',  'U',
//                                     'L',  'T',  'I',  '_',  'A',  'D',  'V',
//                                     '_',  'C',  'O',  'D',  'E',  'D',  0X0};

// static uint8_t legacy_scan_rsp_data[] = {
//     0x02, 0x01, 0x06, 0x02, 0x0a, 0xeb, 0x16, 0x09, 'E', 'S',
//     'P',  '_',  'M',  'U',  'L',  'T',  'I',  '_',  'A', 'D',
//     'V',  '_',  'L',  'E',  'G',  'A',  'C',  'Y',  0X0};

// static uint8_t raw_scan_rsp_data_coded[] = {
//     0x37, 0x09, 'V', 'E', 'R', 'Y', '_', 'L', 'O', 'N', 'G', '_', 'D', 'E',
//     'V',  'I',  'C', 'E', '_', 'N', 'A', 'M', 'E', '_', 'S', 'E', 'N', 'T',
//     '_',  'U',  'S', 'I', 'N', 'G', '_', 'E', 'X', 'T', 'E', 'N', 'D', 'E',
//     'D',  '_',  'A', 'D', 'V', 'E', 'R', 'T', 'I', 'S', 'I', 'N', 'G', 0X0};

// uint8_t addr_1m[6] = {0xc0, 0xde, 0x52, 0x00, 0x00, 0x01};
// uint8_t addr_2m[6] = {0xc0, 0xde, 0x52, 0x00, 0x00, 0x02};
// uint8_t addr_legacy[6] = {0xc0, 0xde, 0x52, 0x00, 0x00, 0x03};
// uint8_t addr_coded[6] = {0xc0, 0xde, 0x52, 0x00, 0x00, 0x04};

// BLEMultiAdvertising advert(4);  // max number of advertisement data

// void setup() {
//     Serial.begin(115200);
//     Serial.println("Multi-Advertising...");

//     BLEDevice::init("");

//     advert.setAdvertisingParams(0, &ext_adv_params_1M);
//     advert.setAdvertisingData(0, sizeof(raw_adv_data_1m),
//     &raw_adv_data_1m[0]); advert.setInstanceAddress(0, addr_1m);
//     advert.setDuration(0);

//     advert.setAdvertisingParams(1, &ext_adv_params_2M);
//     advert.setScanRspData(1, sizeof(raw_scan_rsp_data_2m),
//                           &raw_scan_rsp_data_2m[0]);
//     advert.setInstanceAddress(1, addr_2m);
//     advert.setDuration(1);

//     advert.setAdvertisingParams(2, &legacy_adv_params);
//     advert.setAdvertisingData(2, sizeof(legacy_adv_data),
//     &legacy_adv_data[0]); advert.setScanRspData(2,
//     sizeof(legacy_scan_rsp_data),
//                           &legacy_scan_rsp_data[0]);
//     advert.setInstanceAddress(2, addr_legacy);
//     advert.setDuration(2);

//     advert.setAdvertisingParams(3, &ext_adv_params_coded);
//     advert.setDuration(3);
//     advert.setScanRspData(3, sizeof(raw_scan_rsp_data_coded),
//                           &raw_scan_rsp_data_coded[0]);
//     advert.setInstanceAddress(3, addr_coded);

//     delay(1000);
//     advert.start(4, 0);
// }

// void loop() {
//     delay(2000);
// }
// #endif