#ifndef DATA_PULL_HANDLER_H
#define DATA_PULL_HANDLER_H

#include "imessage_handler.h"
#include "outgoing_message_service.h"
#include "protocol/transport/fragmentation_transport.h"

class DataPullHandler : public IMessageHandler {
public:
    DataPullHandler(OutgoingMessageService& service, FragmentationTransport& transport);
    void process(const uint8_t* requestData, size_t len) override;

private:
    static constexpr const char* TAG = "[DataPullHandler]";
    OutgoingMessageService& _service;
    FragmentationTransport& _transport;
};

#endif  // DATA_PULL_HANDLER_H