package ch.drcookie.polaris_app.domain.interactor

import ch.drcookie.polaris_app.domain.model.dto.PhoneRegistrationRequestDto
import ch.drcookie.polaris_app.domain.repository.AuthRepository
import ch.drcookie.polaris_app.domain.repository.KeyRepository

class RegisterDeviceInteractor(
    private val authRepository: AuthRepository,
    private val keyRepo: KeyRepository
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend operator fun invoke(deviceModel: String, osVersion: String, appVersion: String): Int {
        val (pk, _) = keyRepo.getOrCreateSignatureKeyPair()
        val request = PhoneRegistrationRequestDto(
            publicKey = pk,
            deviceModel = deviceModel,
            osVersion = osVersion,
            appVersion = appVersion
        )
        val beacons = authRepository.registerPhone(request)
        return beacons.size
    }
}