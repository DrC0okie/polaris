#ifndef ROTATE_KEY_FINISH_COMMAND_H
#define ROTATE_KEY_FINISH_COMMAND_H

#include "icommand.h"
#include "utils/key_manager.h"
#include "../../../utils/system_event_notifier.h"

class RotateKeyFinishCommand : public ICommand {
public:
    explicit RotateKeyFinishCommand(KeyManager& keyManager, SystemEventNotifier& notifier);
    CommandResult execute() override;

private:
    KeyManager& _keyManager;
    SystemEventNotifier& _notifier;
};

#endif