package mx.com.gourmeet.app.data.models

data class PlaneadorResponse(
    val success: Boolean,
    val message: String,
    val creado: Boolean?,
    val PLA_ID: Int?,
    val HOG_ID: Int?,
    val PLA_FECHA_INICIO: String?,
    val PLA_FECHA_FIN: String?,
    val espacios: List<EspacioComida>?,
    val dias_cocina: List<DiaCocinaResponse>? = null
)