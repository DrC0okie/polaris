#ifndef SYSTEM_EVENTS_H
#define SYSTEM_EVENTS_H

enum class SystemEventType {
    BeaconReady,
    PoLTokenGenerated,
    ServerCommandReceived,
    KeyRotationInitiated,
    KeyRotationCompleted,
    // Commands
    ServerCmd_Reboot,
    ServerCmd_BlinkLed,
    ServerCmd_StopBlink,
    ServerCmd_RequestStatus,
    BeaconMsg_StatusSent,
    ServerCmd_RotateKeyInit,
    ServerCmd_RotateKeyFinish,
    ServerCmd_NoOp,
    ServerCmd_Unknown,
};

#endif // SYSTEM_EVENTS_H

  