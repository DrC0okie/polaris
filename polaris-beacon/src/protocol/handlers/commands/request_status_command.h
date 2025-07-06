#ifndef REQUEST_STATUS_COMMAND_H
#define REQUEST_STATUS_COMMAND_H

#include "icommand.h"
#include "protocol/handlers/outgoing_message_service.h"
#include "utils/system_monitor.h"

/**
 * @class RequestStatusCommand
 * @brief A command that gathers beacon status and queues it for sending.
 *
 * This command is triggered by a request from the server. Its execution results
 * in a new `BeaconStatus` message being created and queued for delivery back
 * to the server.
 */
class RequestStatusCommand : public ICommand {
public:
    /**
     * @brief Constructs the RequestStatusCommand.
     * @param systemMonitor Reference to the utility for gathering system metrics.
     * @param outgoingMessageService Reference to the service for queuing the response message.
     */
    RequestStatusCommand(SystemMonitor& systemMonitor,
                         OutgoingMessageService& outgoingMessageService);

    /**
     * @brief Executes the command, gathering status and queuing the response.
     */
    CommandResult execute() override;

private:
    /// @brief Reference to the system monitoring utility.
    SystemMonitor& _systemMonitor;

    /// @brief Reference to the outgoing message queuing service.
    OutgoingMessageService& _outgoingMessageService;
};

#endif  // REQUEST_STATUS_COMMAND_H