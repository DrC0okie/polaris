#include "rotate_key_finish_command.h"

RotateKeyFinishCommand::RotateKeyFinishCommand(KeyManager& keyManager,
                                               SystemEventNotifier& notifier)
    : _keyManager(keyManager), _notifier(notifier) {
}

CommandResult RotateKeyFinishCommand::execute() {
    Serial.println("[Command] Executing ROTATE_KEY_FINISH.");
    _notifier.notify(SystemEventType::ServerCmd_RotateKeyFinish);
    CommandResult result;
    result.success = true;
    return result;
}