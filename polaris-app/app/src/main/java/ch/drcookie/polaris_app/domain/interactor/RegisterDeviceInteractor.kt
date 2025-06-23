package ch.drcookie.polaris_app.domain.interactor

import ch.drcookie.polaris_app.domain.interactor.logic.CryptoManager
import ch.drcookie.polaris_app.domain.model.dto.PhoneRegistrationRequestDto
import ch.drcookie.polaris_app.domain.repository.AuthRepository

class RegisterDeviceInteractor(
    private val authRepository: AuthRepository,
    private val cryptoManager: CryptoManager
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend operator fun invoke(deviceModel: String, osVersion: String, appVersion: String): Int {
        // This logic is moved directly from PolarisViewModel.register()
        val (pk, _) = cryptoManager.getOrGeneratePhoneKeyPair()
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