#ifndef BLE_MANAGER_H
#define BLE_MANAGER_H

#include <BLECharacteristic.h>
#include <BLEServer.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <freertos/task.h>

#include <memory>
#include <string>
#include <vector>

#include "../protocol/handlers/imessage_handler.h"
#include "../protocol/messages/encrypted_message.h"
#include "../protocol/messages/pol_request.h"
#include "characteristics/icharacteristic.h"
#include "connectable_advertiser.h"
#include "protocol/handlers/outgoing_message_service.h"
#include "protocol/transport/fragmentation_transport.h"

// Forward declarations
class BLEMultiAdvertising;
class FragmentationTransport;

/// @brief The advertising instance ID for the connectable, legacy advertisement.
static constexpr uint8_t LEGACY_TOKEN_ADV_INSTANCE = 0;

/// @brief The advertising instance ID for the non-connectable, extended advertisement.
static constexpr uint8_t EXTENDED_BROADCAST_ADV_INSTANCE = 1;

/// @brief The total number of advertising instances used by this beacon.
static constexpr uint8_t NUM_ADV_INSTANCES = 2;

/**
 * @class BleManager
 * @brief The central controller for all BLE functionality.
 *
 * This class manes the entire BLE stack, including the GATT server,
 * services, characteristics, and multi-set advertising. It uses a queue-per-task
 * model to process incoming requests from different characteristics asynchronously.
 */
class BleManager {
public:
    /**
     * @brief Constructs the BleManager.
     */
    explicit BleManager();
    ~BleManager();

    /**
     * @brief Initializes and starts all BLE services and advertising.
     * @param deviceName The public name for the device, used in scan responses.
     */
    void begin(const std::string& deviceName);

    /**
     * @brief Stops all BLE activity and cleans up resources.
     */
    void stop();

    /** @brief Queues a raw token request from a BLE write event for processing. */
    void queueTokenRequest(const uint8_t* data, size_t len);

    /** @brief Queues a raw encrypted request from a BLE write event for processing. */
    void queueEncryptedRequest(const uint8_t* data, size_t len);

    /** @brief Queues a pull request trigger from a BLE write event for processing. */
    void queuePullRequest();

    /** @brief Registers the handler for processed token requests. */
    void setTokenRequestProcessor(IMessageHandler* processor);

    /** @brief Registers the transport layer for processed encrypted requests. */
    void setEncryptedDataProcessor(FragmentationTransport* transport);

    /** @brief Registers the handler for processed data pull requests. */
    void setPullRequestProcessor(IMessageHandler* processor);

    /** @brief Injects the dependency for the outgoing message service. */
    void setOutgoingMessageService(OutgoingMessageService* service);

    /** @brief Registers a transport layer to receive MTU update notifications. */
    void registerTransportForMtuUpdates(FragmentationTransport* transport);

    /** @brief gets a raw characteristic pointer by its UUID. */
    BLECharacteristic* getCharacteristicByUUID(const BLEUUID& targetUuid) const;

    /** @brief Gets a pointer to the multi-advertising controller. */
    BLEMultiAdvertising* getMultiAdvertiser();

    // --- Service and Characteristic UUIDs --
    static constexpr const char* POL_SERVICE = "f44dce36-ffb2-565b-8494-25fa5a7a7cd6";
    static constexpr const char* TOKEN_WRITE = "8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b";
    static constexpr const char* TOKEN_INDICATE = "d234a7d8-ea1f-5299-8221-9cf2f942d3df";
    static constexpr const char* ENCRYPTED_WRITE = "8ed72380-5adb-4d2d-81fb-ae6610122ee8";
    static constexpr const char* ENCRYPTED_INDICATE = "079b34dd-2310-4b61-89bb-494cc67e097f";
    static constexpr const char* PULL_DATA_WRITE = "e914a8e4-843a-4b72-8f2a-f9175d71cf88";

private:
    BleManager(const BleManager&) = delete;
    BleManager& operator=(const BleManager&) = delete;
    BleManager(BleManager&&) = delete;
    BleManager& operator=(BleManager&&) = delete;

    /**
     * @class ServerCallbacks
     * @brief An inner class to handle global BLE server events.
     */
    class ServerCallbacks : public BLEServerCallbacks {
    public:
        explicit ServerCallbacks(BleManager* parentServer);
        void onConnect(BLEServer* pServer) override;
        void onDisconnect(BLEServer* pServer) override;
        void onMtuChanged(BLEServer* pServer, esp_ble_gatts_cb_param_t* param) override;

    private:
        BleManager* _parentManager;
    };

    /// @brief A message structure for the token request queue.
    struct TokenRequestMessage {
        uint8_t data[512];
        size_t len;
    };

    /// @brief A message structure for the encrypted request queue.
    struct EncryptedRequestMessage {
        uint8_t data[512];
        size_t len;
    };

    /// @brief The handler for unencrypted token messages.
    IMessageHandler* _tokenRequestProcessor = nullptr;

    /// @brief The FreeRTOS task handle for the token processor.
    TaskHandle_t _tokenProcessorTask = nullptr;

    /// @brief The FreeRTOS queue for incoming token requests.
    QueueHandle_t _tokenQueue = nullptr;

    /// @brief The transport layer for the encrypted message channel.
    FragmentationTransport* _encryptedDataTransport = nullptr;

    /// @brief The FreeRTOS task handle for the encrypted data processor.
    TaskHandle_t _encryptedProcessorTask = nullptr;

    /// @brief The FreeRTOS queue for incoming encrypted requests.
    QueueHandle_t _encryptedQueue = nullptr;

    /// @brief The handler for data pull requests.
    IMessageHandler* _pullRequestProcessor = nullptr;

    /// @brief The FreeRTOS task handle for the data pull processor.
    TaskHandle_t _pullProcessorTask = nullptr;

    /// @brief The FreeRTOS queue for incoming data pull triggers.
    QueueHandle_t _pullQueue = nullptr;

    /// @brief A pointer to the service managing outgoing message queues.
    OutgoingMessageService* _outgoingMessageService = nullptr;

    /// @brief A list of transport layers that need to be notified of MTU changes.
    std::vector<FragmentationTransport*> _transportsForMtuUpdate;

    /// @brief A pointer to the main BLE server instance.
    BLEServer* _pServer = nullptr;

    /// @brief A unique pointer to the server callback handler instance.
    std::unique_ptr<ServerCallbacks> _serverCallbacks;

    /// @brief A flag to signal all processor tasks to shut down.
    volatile bool _shutdownRequested = false;

    /// @brief A unique pointer to the multi-advertising controller.
    std::unique_ptr<BLEMultiAdvertising> _multiAdvertiserPtr;

    /// @brief A vector that owns all the characteristic wrapper objects.
    std::vector<std::unique_ptr<ICharacteristic>> _polServiceChars;

    /** @brief The FreeRTOS task function for processing token requests. */
    static void tokenProcessorTask(void* pvParameters);

    /** @brief The instance method containing the token processor task main loop. */
    void processTokenRequests();

    /** @brief The FreeRTOS task function for processing encrypted requests. */
    static void encryptedProcessorTask(void* pvParameters);

    /** @brief The instance method containing the encrypted processor task main loop. */
    void processEncryptedRequests();

    /** @brief The FreeRTOS task function for processing data pull requests. */
    static void pullProcessorTask(void* pvParameters);

    /** @brief The instance method containing the pull processor task main loop. */
    void processPullRequests();

    /** @brief Sets up the parameters for the connectable (legacy) advertisement. */
    bool configureTokenSrvcAdvertisement(const std::string& deviceName, uint8_t instanceNum,
                                         const char* serviceUuid);

    /** @brief Sets up the parameters for the non-connectable (extended) advertisement. */
    bool configureExtendedAdvertisement();

    /** @brief Notifies registered listeners of an MTU change. */
    void updateMtu(uint16_t newMtu);
};

#endif  // BLE_MANAGER_H