#include "request_status_command.h"

#include <ArduinoJson.h>
#include <HardwareSerial.h>

RequestStatusCommand::RequestStatusCommand(SystemMonitor& systemMonitor,
                                           OutgoingMessageService& outgoingMessageService)
    : _systemMonitor(systemMonitor), _outgoingMessageService(outgoingMessageService) {
}

CommandResult RequestStatusCommand::execute() {
    Serial.println("[Command] Executing REQUEST_BEACON_STATUS.");

    // Create a JSON document to hold the status payload.
    JsonDocument doc;
    JsonObject statusPayload = doc.to<JsonObject>();

    // Use the SystemMonitor to populate the JSON object.
    _systemMonitor.getStatus(statusPayload);

    // Queue the new message for sending back to the server.
    _outgoingMessageService.queueMessage(OperationType::BeaconStatus, statusPayload);

    CommandResult result;
    result.success = true;
    return result;
}