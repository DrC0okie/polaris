#ifndef ENCRYPTED_MESSAGE_HANDLER_H
#define ENCRYPTED_MESSAGE_HANDLER_H

#include <Preferences.h>  // For NVS

#include "../../utils/counter.h"
#include "../../utils/crypto_service.h"
#include "../messages/encrypted_message.h"
#include "../pol_constants.h"
#include "commands/command_factory.h"
#include "imessage_handler.h"
#include "protocol/transport/imessage_transport.h"

class EncryptedMessageHandler : public IMessageHandler {
public:
    EncryptedMessageHandler(const CryptoService& cryptoService,
                            const MinuteCounter& beaconEventCounter,
                            Preferences& prefs,  // Pass NVS preferences
                            IMessageTransport& transport, CommandFactory& commandFactory);

    void process(const uint8_t* encryptedData, size_t len) override;

private:
    static constexpr const char* TAG = "[EncMessageHandler]";
    IMessageTransport& _transport;
    uint32_t _beaconIdForAd;
    const CryptoService& _cryptoService;
    const MinuteCounter& _beaconEventCounter;
    Preferences& _prefs;              // Store reference to NVS
    CommandFactory& _commandFactory;

    uint32_t _nextResponseMsgId;  // Manages unique msgId for responses from this beacon

    void loadNextResponseMsgId();
    void saveNextResponseMsgId();

    void sendAck(uint32_t originalMsgId, uint8_t originalOpType);
    void sendErr(uint32_t originalMsgId, uint8_t originalOpType, uint8_t errorCode);
    void handleIncomingCommand(const InnerPlaintext& pt);
};

#endif  // ENCRYPTED_MESSAGE_HANDLER_H