#include "beacon_broadcaster.h"

#include <esp_gap_ble_api.h>
#include <esp_log.h>
#include <freertos/FreeRTOS.h>  // For vTaskDelay
#include <freertos/task.h>      // For vTaskDelay
#include <string.h>             // For memcpy

#include "../protocol/crypto.h"

static const char* TAG = "BEACON_BC_EXT";  // Changed tag slightly

#ifndef CONFIG_BT_BLE_50_FEATURES_SUPPORTED
#error "BLE 5.0 Features are required!"
#endif

// Constructor
BeaconBroadcaster::BeaconBroadcaster(MinuteCounter& counter_ref)
    : _bleMultiAdv(1), _counter(counter_ref) {
    _isRunning = false;
}

// Destructor
BeaconBroadcaster::~BeaconBroadcaster() {
    // stop();
}

// --- Simplified begin() for Extended Advertising ONLY ---
bool BeaconBroadcaster::begin(uint32_t beacon_id, const uint8_t sk[32],
                              const uint8_t pk[32], uint32_t interval_ms) {
    // if (_isRunning) {
    //     ESP_LOGW(TAG, "Broadcaster already running.");
    //     return true;
    // }

    // ESP_LOGI(TAG, "Starting Extended Broadcaster with interval %lu ms",
    //          interval_ms);

    // _beacon_id = beacon_id;
    // memcpy(_sk, sk, 32);
    // memcpy(_pk, pk, 32);
    // _currentIntervalMs = interval_ms;

    // esp_err_t err;

    // // --- Step 1: Generate and Set Random Static Address ---
    // esp_bd_addr_t rand_addr;
    // esp_fill_random(rand_addr,
    //                 sizeof(esp_bd_addr_t));  // Fill with random bytes
    // // Ensure top two bits are '1' for Static Random Address
    // rand_addr[0] |= 0xC0;  // Set bits 6 and 7 of the most significant byte

    // ESP_LOGI(TAG, "Generated static random addr: %02x:%02x:%02x:%02x:%02x:%02x",
    //          rand_addr[0], rand_addr[1], rand_addr[2], rand_addr[3],
    //          rand_addr[4], rand_addr[5]);

    // // Use the correct IDF function for extended advertising instances
    // err = esp_ble_gap_ext_adv_set_rand_addr(0, rand_addr);  // Instance 0
    // if (err != ESP_OK) {
    //     ESP_LOGE(TAG, "Failed esp_ble_gap_ext_adv_set_rand_addr: %s (0x%X)",
    //              esp_err_to_name(err), err);
    //     return false;
    // }
    // ESP_LOGD(TAG, "Set random address command issued.");
    // vTaskDelay(pdMS_TO_TICKS(100));  // HACK: Delay

    // --- Step 2: Configure Extended Advertising Parameters ---
    // Use own_addr_type = BLE_ADDR_TYPE_RANDOM
    esp_ble_gap_ext_adv_params_t ext_adv_params = {0};
    ext_adv_params.type = ESP_BLE_GAP_SET_EXT_ADV_PROP_SCANNABLE;
    ext_adv_params.interval_min = 0x50;  // Example value (30ms)
    ext_adv_params.interval_max = 0x50;  // Example value (30ms)
    ext_adv_params.channel_map = ADV_CHNL_ALL;
    ext_adv_params.filter_policy = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY;
    ext_adv_params.primary_phy = ESP_BLE_GAP_PHY_1M;
    ext_adv_params.secondary_phy = ESP_BLE_GAP_PHY_CODED;
    ext_adv_params.tx_power = EXT_ADV_TX_PWR_NO_PREFERENCE;
    ext_adv_params.sid = 0;
    ext_adv_params.scan_req_notif = false;
    ext_adv_params.max_skip = 0;
    ext_adv_params.own_addr_type = BLE_ADDR_TYPE_RANDOM;
    ext_adv_params.peer_addr_type = BLE_ADDR_TYPE_RANDOM;

    uint8_t addr_coded[6] = {0xc0, 0xde, 0x52, 0x00, 0x00, 0x04};

    static uint8_t raw_scan_rsp_data_coded[] = {0x37, 0x09, 'V', 'E', 'R', 'Y', '_', 'L', 'O', 'N', 'G', '_', 'D', 'E', 'V', 'I', 'C', 'E', '_',
        'N',  'A',  'M', 'E', '_', 'S', 'E', 'N', 'T', '_', 'U', 'S', 'I', 'N', 'G', '_', 'E', 'X', 'T',
        'E',  'N',  'D', 'E', 'D', '_', 'A', 'D', 'V', 'E', 'R', 'T', 'I', 'S', 'I', 'N', 'G', 0X0};

    _bleMultiAdv.setAdvertisingParams(3, &ext_adv_params);
    _bleMultiAdv.setDuration(3);
    _bleMultiAdv.setScanRspData(3, sizeof(raw_scan_rsp_data_coded), &raw_scan_rsp_data_coded[0]);
    _bleMultiAdv.setInstanceAddress(3, addr_coded);

    return true;

    // ESP_LOGI(TAG, "Setting extended advertising parameters...");
    // err = esp_ble_gap_ext_adv_set_params(0, &ext_adv_params);
    // if (err != ESP_OK) {
    //     ESP_LOGE(TAG, "Failed set_params command: %s (0x%X)",
    //              esp_err_to_name(err), err);
    //     return false;
    // }
    // ESP_LOGD(TAG, "Set extended params command issued.");
    // vTaskDelay(pdMS_TO_TICKS(100));  // HACK: Delay

    // // --- Step 3: Set Initial Extended Advertising Data ---
    // updateAndSetExtAdvData();  // Prepare the first payload
    // ESP_LOGI(TAG, "Setting initial extended advertising data (%d bytes)...",
    //          BROADCAST_PAYLOAD_SIZE);
    // err = esp_ble_gap_config_ext_adv_data_raw(0, BROADCAST_PAYLOAD_SIZE,
    //                                           _broadcastPayload);
    // if (err != ESP_OK) {
    //     ESP_LOGE(TAG, "Failed config_ext_adv_data_raw command: %s (0x%X)",
    //              esp_err_to_name(err), err);
    //     return false;
    // }
    // ESP_LOGD(TAG, "Set extended data command issued.");
    // vTaskDelay(pdMS_TO_TICKS(100));  // HACK: Delay

    // // --- Step 4: Start Extended Advertising ---
    // esp_ble_gap_ext_adv_t ext_adv_cmd_params = {0};
    // ext_adv_cmd_params.instance = 0;
    // ext_adv_cmd_params.duration = 0;
    // ext_adv_cmd_params.max_events = 0;

//     ESP_LOGI(TAG, "Starting extended advertising (Instance 0)...");
//     err = esp_ble_gap_ext_adv_start(1, &ext_adv_cmd_params);
//     if (err != ESP_OK) {
//         ESP_LOGE(TAG, "Failed ext_adv_start command: %s (0x%X)",
//                  esp_err_to_name(err), err);
//         return false;
//     }
//     ESP_LOGI(TAG, "Extended advertising start command issued.");
//     vTaskDelay(pdMS_TO_TICKS(100));  // HACK: Delay

//     // --- Step 5: Start Update Timer ---
//     if (_currentIntervalMs < 100) {
//         ESP_LOGW(TAG, "Update interval too short (%lu ms), setting to 1000ms",
//                  _currentIntervalMs);
//         _currentIntervalMs = 1000;
//     }
//     _updateTimer.attach_ms(_currentIntervalMs, timer_callback, this);
//     ESP_LOGI(TAG, "Update timer started for %lu ms interval.",
//              _currentIntervalMs);

//     _isRunning = true;
//     ESP_LOGI(TAG, "Extended Broadcaster started successfully.");
//     return true;
// }

// // Stop broadcasting
// void BeaconBroadcaster::stop() {
//     if (!_isRunning) {
//         return;
//     }
//     ESP_LOGI(TAG, "Stopping extended broadcaster...");

//     _updateTimer.detach();
//     ESP_LOGD(TAG, "Update timer stopped.");

//     // Stop extended advertising set
//     uint8_t instance = 0;
//     // Use direct IDF call for consistency
//     esp_err_t err = esp_ble_gap_ext_adv_stop(1, &instance);
//     if (err != ESP_OK && err != ESP_ERR_INVALID_STATE) {
//         ESP_LOGE(TAG, "Failed ext_adv_stop command: %s (0x%X)",
//                  esp_err_to_name(err), err);
//     } else {
//         ESP_LOGD(TAG, "Extended advertising stop command issued (Instance 0).");
//         // Ideally wait for ESP_GAP_BLE_EXT_ADV_STOP_COMPLETE_EVT here
//     }
//     vTaskDelay(pdMS_TO_TICKS(100));  // HACK: Delay after stop

//     // Optional: Remove the advertising set configuration
//     // err = esp_ble_gap_ext_adv_set_remove(0);
//     // ... (error checking) ...

//     _isRunning = false;
//     ESP_LOGI(TAG, "Extended Broadcaster stopped.");
// }

// // Static timer callback
// void BeaconBroadcaster::timer_callback(BeaconBroadcaster* instance) {
//     if (instance) {
//         instance->updateAndSetExtAdvData();
//     }
// }

// // --- Renamed: Update payload and set it for EXTENDED advertising ---
// void BeaconBroadcaster::updateAndSetExtAdvData() {
//     if (!_isRunning)
//         return;  // Don't try to update if not fully running

//     ESP_LOGD(TAG, "Updating extended advertising payload...");

//     uint64_t current_counter = _counter.getValue();
//     uint8_t signature[POL_SIG_SIZE];

//     // 1. Sign the data
//     signBeaconBroadcast(signature, _beacon_id, current_counter, _sk, _pk);

//     // 2. Serialize into the buffer
//     size_t offset = 0;
//     memcpy(_broadcastPayload + offset, &_beacon_id, sizeof(_beacon_id));
//     offset += sizeof(_beacon_id);
//     memcpy(_broadcastPayload + offset, &current_counter,
//            sizeof(current_counter));
//     offset += sizeof(current_counter);
//     memcpy(_broadcastPayload + offset, signature, sizeof(signature));

//     // 3. Set the data for *EXTENDED* advertising (Instance 0)
//     // Use direct IDF call
//     esp_err_t err = esp_ble_gap_config_ext_adv_data_raw(
//         0, BROADCAST_PAYLOAD_SIZE, _broadcastPayload);
//     if (err != ESP_OK) {
//         ESP_LOGE(TAG, "Failed config_ext_adv_data_raw command: %s (0x%X)",
//                  esp_err_to_name(err), err);
//         // Check HCI logs for async errors
//     } else {
//         ESP_LOGD(
//             TAG,
//             "Extended advertising data update command issued (Counter: %llu)",
//             current_counter);
//         // Ideally wait for ESP_GAP_BLE_EXT_ADV_DATA_SET_COMPLETE_EVT
//     }
}