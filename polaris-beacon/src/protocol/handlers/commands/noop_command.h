#ifndef NOOP_COMMAND_H
#define NOOP_COMMAND_H

#include "icommand.h"
#include "../../../utils/system_event_notifier.h"

class NoOpCommand : public ICommand {
public:
    explicit NoOpCommand(SystemEventNotifier& notifier);
    CommandResult execute() override;

private:
    SystemEventNotifier& _notifier;
};

#endif // NOOP_COMMAND_H