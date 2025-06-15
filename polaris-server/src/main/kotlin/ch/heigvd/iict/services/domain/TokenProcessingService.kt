package ch.heigvd.iict.services.domain

import ch.heigvd.iict.dto.api.PoLTokenDto
import ch.heigvd.iict.dto.api.PoLTokenValidationResultDto
import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.entities.PoLTokenRecord
import ch.heigvd.iict.entities.RegisteredPhone
import ch.heigvd.iict.repositories.BeaconRepository
import ch.heigvd.iict.repositories.PoLTokenRecordRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class TokenProcessingService @Inject constructor(
    private val beaconRepo: BeaconRepository,
    private val recordRepo: PoLTokenRecordRepository,
    private val validator: PoLTokenValidator,
    private val assembler: PoLTokenAssembler
) {
    fun process(dto: PoLTokenDto, phone: RegisteredPhone): PoLTokenValidationResultDto {
        val beacon = beaconRepo.findByBeaconTechnicalId(dto.beaconId.toInt())
            ?: throw NotFoundException("Beacon ${dto.beaconId} not found")

        val errors = validator.validate(dto, phone, beacon)
        val isValid = errors.isEmpty()
        val record = assembler.assemble(dto, phone, beacon, isValid, errors)

        persistRecordAndUpdateCounter(record, beacon, dto.beaconCounter.toLong())

        return PoLTokenValidationResultDto(
            isValid = isValid,
            message = if (isValid) null else errors.joinToString("; "),
            id = record.id
        )
    }

    @Transactional
    fun persistRecordAndUpdateCounter( record: PoLTokenRecord,  beacon: Beacon, newCounter: Long ) {
        recordRepo.persist(record)
        if (record.isValid && newCounter > beacon.lastKnownCounter) {
            beacon.lastKnownCounter = newCounter
        }
    }
}
