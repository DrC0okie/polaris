package ch.heigvd.iict.services.payload

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.services.crypto.model.*

interface IMessageSealer {
    fun seal(plaintext: PlaintextMessage, targetBeacon: Beacon): SealedMessage
}