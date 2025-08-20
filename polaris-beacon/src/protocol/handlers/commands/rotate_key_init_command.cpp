#include "rotate_key_init_command.h"

#include "protocol/pol_constants.h"

RotateKeyInitCommand::RotateKeyInitCommand(KeyManager& keyManager, SystemEventNotifier& notifier)
    : _keyManager(keyManager), _notifier(notifier) {
}

CommandResult RotateKeyInitCommand::execute() {
    Serial.println("[Command] Executing ROTATE_KEY_INIT.");
    _notifier.notify(SystemEventType::ServerCmd_RotateKeyInit);

    _keyManager.prepareNewX25519KeyPair();
    const uint8_t* newPk = _keyManager.getNewX25519Pk();
    CommandResult result;
    result.success = true;
    result.responsePayload.assign(newPk, newPk + X25519_PK_SIZE);
    return result;
}