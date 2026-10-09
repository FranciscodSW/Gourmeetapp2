package mx.com.gourmeet.app.data.models

data class AlacenaResponse(
    val success: Boolean,
    val message: String?,
    val ALC_ID: Int?,
    val ALC_CLI_ID: Int?,
    val ALC_NOMBRE: String?,
    val ALC_ICONO: String?
)