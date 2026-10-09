package mx.com.gourmeet.app.data.models

data class CrearAlacenaHogarRequest(
    val HOG_ID: Int,
    val ALC_CLI_ID: Int?,
    val ALC_NOMBRE: String,
    val ALC_ICONO: String
)