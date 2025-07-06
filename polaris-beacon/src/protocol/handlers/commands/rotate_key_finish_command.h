#ifndef ROTATE_KEY_FINISH_COMMAND_H
#define ROTATE_KEY_FINISH_COMMAND_H

#include "icommand.h"
#include "utils/key_manager.h"

class RotateKeyFinishCommand : public ICommand {
public:
    explicit RotateKeyFinishCommand(KeyManager& keyManager);
    CommandResult execute() override;

private:
    KeyManager& _keyManager;
};

#endif