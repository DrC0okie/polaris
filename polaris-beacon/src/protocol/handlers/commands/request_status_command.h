#ifndef REQUEST_STATUS_COMMAND_H
#define REQUEST_STATUS_COMMAND_H

#include "icommand.h"
#include "protocol/handlers/outgoing_message_service.h"
#include "utils/system_monitor.h"

class RequestStatusCommand : public ICommand {
public:
    RequestStatusCommand(SystemMonitor& systemMonitor,
                         OutgoingMessageService& outgoingMessageService);
    void execute() override;

private:
    SystemMonitor& _systemMonitor;
    OutgoingMessageService& _outgoingMessageService;
};

#endif  // REQUEST_STATUS_COMMAND_H