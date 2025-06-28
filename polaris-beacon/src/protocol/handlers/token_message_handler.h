#ifndef TOKEN_HANDLER_H
#define TOKEN_HANDLER_H

#include "../../utils/beacon_counter.h"
#include "../../utils/crypto_service.h"
#include "../pol_constants.h"
#include "imessage_handler.h"
#include "protocol/transport/imessage_transport.h"

/**
 * @class TokenMessageHandler
 * @brief Handles unencrypted Proof-of-Location (PoL) token requests.
 *
 * This handler is responsible for processing an incoming PoLRequest, verifying its signature, and
 * constructing a signed PoLResponse containing the beacon current counter value.
 */
class TokenMessageHandler : public IMessageHandler {
public:
    /**
     * @brief Constructs the TokenMessageHandler.
     * @param cryptoService Reference to the cryptographic service for signing/verification.
     * @param counter Reference to the beacon counter.
     * @param transport Reference to the transport layer for sending the response.
     */
    TokenMessageHandler(const CryptoService& cryptoService, const BeaconCounter& counter,
                        IMessageTransport& transport);

    /**
     * @brief Processes a complete, reassembled PoL token request.
     * @param requestData The raw binary data of the `PoLRequest`.
     * @param len The length of the request data.
     */
    void process(const uint8_t* requestData, size_t len) override;

private:
    /// @brief A reference to the cryptographic service.
    const CryptoService& _cryptoService;

    /// @brief A reference to the beacon counter.
    const BeaconCounter& _counter;

    /// @brief The transport layer used to send the signed response.
    IMessageTransport& _transport;
};

#endif  // TOKEN_HANDLER_H