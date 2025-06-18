package ch.heigvd.iict.services.crypto

import ch.heigvd.iict.entities.Beacon

interface ISharedKeyManager {
    fun getSharedKeyForBeacon(beacon: Beacon): ByteArray
}