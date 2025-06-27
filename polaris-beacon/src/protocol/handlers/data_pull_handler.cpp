#include "data_pull_handler.h"

#include <HardwareSerial.h>

DataPullHandler::DataPullHandler(OutgoingMessageService& service, FragmentationTransport& transport)
    : _service(service), _transport(transport) {
}

void DataPullHandler::process(const uint8_t* requestData, size_t len) {
    Serial.printf("%s: Processing pull request.\n", TAG);
    if (_service.hasPendingMessages()) {
        std::vector<uint8_t> message = _service.getNextMessageForSending();
        if (!message.empty()) {
            Serial.printf("%s: Sending queued message of size %zu.\n", TAG, message.size());
            _transport.sendMessage(message.data(), message.size());
        }
    } else {
        Serial.printf("%s: Pull request received, but outgoing queue is empty.\n", TAG);
    }
}