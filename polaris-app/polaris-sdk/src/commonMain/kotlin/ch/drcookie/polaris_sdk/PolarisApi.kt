package ch.drcookie.polaris_sdk

import ch.drcookie.polaris_sdk.domain.repository.*

/**
 * Defines the public, read-only API surface of the Polaris SDK.
 */
interface PolarisApi {
    val authRepository: AuthRepository
    val keyRepository: KeyRepository
    val protocolRepository: ProtocolRepository
    val bleDataSource: BleDataSource
}