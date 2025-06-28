#ifndef ENCRYPTED_MESSAGE_HANDLER_H
#define ENCRYPTED_MESSAGE_HANDLER_H

#include <Preferences.h>  // For NVS

#include "../../utils/beacon_counter.h"
#include "../../utils/crypto_service.h"
#include "../messages/encrypted_message.h"
#include "../pol_constants.h"
#include "commands/command_factory.h"
#include "imessage_handler.h"
#include "outgoing_message_service.h"
#include "protocol/transport/imessage_transport.h"

/**
 * @class EncryptedMessageHandler
 * @brief The primary handler for all encrypted, bidirectional communication.
 *
 * Manages incoming encrypted messages. It decrypts the payload, determines the message type
 * and delegates the action to the appropriate component (CommandFactory for requests,
 * OutgoingMessageService for ACKs).
 */
class EncryptedMessageHandler : public IMessageHandler {
public:
    /**
     * @brief Constructs the EncryptedMessageHandler.
     * @param cryptoService Reference to the cryptographic service.
     * @param beaconEventCounter Reference to the beacon main counter.
     * @param prefs Reference to the NVS storage object.
     * @param transport Reference to the transport layer used for sending responses.
     * @param commandFactory Reference to the factory for creating command objects.
     * @param outgoingMessageService Reference to the service that manages beacon-originated
     * messages.
     */
    EncryptedMessageHandler(const CryptoService& cryptoService,
                            const BeaconCounter& beaconEventCounter,
                            Preferences& prefs,  // Pass NVS preferences
                            IMessageTransport& transport, CommandFactory& commandFactory,
                            OutgoingMessageService& outgoingMessageService);

    /**
     * @brief Processes the encrypted message from the transport layer.
     * @param encryptedData The raw encrypted message payload.
     * @param len The length of the payload.
     */
    void process(const uint8_t* encryptedData, size_t len) override;

private:
    /// @brief A tag used for logging from this class.
    static constexpr const char* TAG = "[EncMessageHandler]";

    /// @brief The transport layer for sending responses.
    IMessageTransport& _transport;

    /// @brief The beacon ID, used as Associated Data in encrypted messages.
    uint32_t _beaconIdForAd;

    /// @brief A reference to the cryptographic service.
    const CryptoService& _cryptoService;

    /// @brief A reference to the beacon main counter.
    const BeaconCounter& _beaconEventCounter;

    /// @brief A reference to the NVS storage object.
    Preferences& _prefs;

    /// @brief A reference to the factory used to create command objects from incoming requests.
    CommandFactory& _commandFactory;

    /// @brief A reference to the service managing outgoing messages and their ACKs.
    OutgoingMessageService& _outgoingMessageService;

    /// @brief A persistent counter for the `msgId` of outgoing ACKs/ERRs.
    uint32_t _nextResponseMsgId;

    /** @brief Loads the response message ID counter from NVS. */
    void loadNextResponseMsgId();

    /** @brief Saves the response message ID counter to NVS. */
    void saveNextResponseMsgId();

    /** @brief Constructs and sends an ACK message in response to a request. */
    void sendAck(uint32_t originalMsgId, uint8_t originalOpType);

    /** @brief Constructs and sends an ERR message in response to a request. */
    void sendErr(uint32_t originalMsgId, uint8_t originalOpType, uint8_t errorCode);

    /** @brief Handles a decrypted incoming REQ by executing a command object. */
    void handleIncomingCommand(const InnerPlaintext& pt);
};

#endif  // ENCRYPTED_MESSAGE_HANDLER_H