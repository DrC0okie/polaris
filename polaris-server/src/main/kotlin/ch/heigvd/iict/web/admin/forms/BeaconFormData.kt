package ch.heigvd.iict.web.admin.forms

// A simple data class to hold the raw data from the form.
data class BeaconFormData(
    val technicalId: String?,
    val name: String?,
    val locationDescription: String?,
    val publicKeyEd25519Hex: String?,
    val publicKeyX25519Hex: String?
)