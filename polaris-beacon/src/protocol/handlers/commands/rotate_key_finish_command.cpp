#include "rotate_key_finish_command.h"

RotateKeyFinishCommand::RotateKeyFinishCommand(KeyManager& keyManager) : _keyManager(keyManager) {
}

CommandResult RotateKeyFinishCommand::execute() {
    Serial.println("[Command] Executing ROTATE_KEY_FINISH.");
    // For now, do nothing

    CommandResult result;
    result.success = true;
    return result;
}