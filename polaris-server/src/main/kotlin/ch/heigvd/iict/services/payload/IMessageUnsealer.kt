package ch.heigvd.iict.services.payload

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.services.crypto.model.*

interface IMessageUnsealer {
    fun unseal(sealed: SealedMessage, sourceBeacon: Beacon): PlaintextMessage
}