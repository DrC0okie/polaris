#include "rotate_key_init_command.h"

#include "protocol/pol_constants.h"

RotateKeyInitCommand::RotateKeyInitCommand(KeyManager& keyManager) : _keyManager(keyManager) {
}

CommandResult RotateKeyInitCommand::execute() {
    Serial.println("[Command] Executing ROTATE_KEY_INIT.");

    _keyManager.prepareNewX25519KeyPair();
    const uint8_t* newPk = _keyManager.getNewX25519Pk();
    CommandResult result;
    result.success = true;
    result.responsePayload.assign(newPk, newPk + X25519_PK_SIZE);
    return result;
}