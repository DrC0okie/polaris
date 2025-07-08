package ch.heigvd.iict.services.token

import ch.heigvd.iict.dto.api.PoLTokenDto
import ch.heigvd.iict.dto.api.PoLTokenValidationResultDto
import ch.heigvd.iict.entities.RegisteredPhone
import ch.heigvd.iict.repositories.BeaconRepository
import ch.heigvd.iict.repositories.PoLTokenRecordRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException

/**
 * Service that manages the entire PoL token processing workflow.
 *
 * This service acts as a facade, coordinating the validation, assembly, and persistence
 * of a submitted PoL token.
 *
 * @property beaconRepo Repository for fetching beacon data.
 * @property recordRepo Repository for persisting the final token record.
 * @property validator The service that performs all validation checks.
 * @property assembler The service that maps the DTO to a persistent entity.
 */
@ApplicationScoped
class TokenProcessingService @Inject constructor(
    private val beaconRepo: BeaconRepository,
    private val recordRepo: PoLTokenRecordRepository,
    private val validator: PoLTokenValidator,
    private val assembler: PoLTokenAssembler
) {
    /**
     * Processes a submitted PoL token from a mobile client.
     *
     * @param dto The token DTO submitted by the client.
     * @param phone The authenticated [RegisteredPhone] that submitted the token.
     * @return A [PoLTokenValidationResultDto] indicating if the token was valid and a list of any errors.
     * @throws jakarta.ws.rs.NotFoundException if the beacon specified in the token does not exist.
     */
    @Transactional
    fun process(dto: PoLTokenDto, phone: RegisteredPhone): PoLTokenValidationResultDto {
        val beacon = beaconRepo.findByBeaconTechnicalId(dto.beaconId.toInt())
            ?: throw NotFoundException("Beacon ${dto.beaconId} not found")

        val errors = validator.validate(dto, phone, beacon)
        val isValid = errors.isEmpty()
        val record = assembler.assemble(dto, phone, beacon, isValid, errors)
        val newCounter = dto.beaconCounter.toLong()
        recordRepo.persist(record)

        if (isValid && newCounter > beacon.lastKnownCounter) {
            beacon.lastKnownCounter = newCounter
        }

        return PoLTokenValidationResultDto(
            isValid = isValid,
            message = if (isValid) null else errors.joinToString("; "),
            id = record.id
        )
    }
}
