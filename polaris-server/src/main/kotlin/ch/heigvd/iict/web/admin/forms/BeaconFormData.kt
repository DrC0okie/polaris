package ch.heigvd.iict.web.admin.forms

/**
 * Holds the raw, unvalidated string data submitted from an HTML form
 * for creating or editing a beacon.
 *
 * @property technicalId The technical ID of the beacon as a string.
 * @property name The friendly name of the beacon.
 * @property locationDescription The location description.
 * @property publicKeyEd25519Hex The Ed25519 public key as a hexadecimal string.
 * @property publicKeyX25519Hex The X25519 public key as a hexadecimal string.
 */
data class BeaconFormData(
    val technicalId: String?,
    val name: String?,
    val locationDescription: String?,
    val publicKeyEd25519Hex: String?,
    val publicKeyX25519Hex: String?
)