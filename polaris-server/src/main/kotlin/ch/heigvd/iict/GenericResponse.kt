package ch.heigvd.iict

import kotlinx.serialization.Serializable

@Serializable
data class GenericResponse(    val status: String,
                               val message: String)
