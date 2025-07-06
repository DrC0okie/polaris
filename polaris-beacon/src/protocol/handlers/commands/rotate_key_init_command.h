#ifndef ROTATE_KEY_INIT_COMMAND_H
#define ROTATE_KEY_INIT_COMMAND_H

#include "icommand.h"
#include "protocol/handlers/encrypted_message_handler.h"
#include "utils/key_manager.h"

class RotateKeyInitCommand : public ICommand {
public:
    RotateKeyInitCommand(KeyManager& keyManager);
    CommandResult execute() override;

private:
    KeyManager& _keyManager;
    uint32_t _originalMsgId;
    uint8_t _originalOpType;
};

#endif