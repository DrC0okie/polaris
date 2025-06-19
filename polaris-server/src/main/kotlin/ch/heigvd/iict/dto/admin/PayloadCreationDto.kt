package ch.heigvd.iict.dto.admin

import ch.heigvd.iict.entities.Beacon

data class PayloadCreationDto(
    // A list of all beacons to populate the dropdown menu.
    val beacons: List<Beacon>,
    // The following fields are used to re-populate the form if there was a submission error.
    val selectedBeaconId: Int?,
    val opType: Int?,
    val commandPayload: String?,
    val redundancyFactor: Int?
)