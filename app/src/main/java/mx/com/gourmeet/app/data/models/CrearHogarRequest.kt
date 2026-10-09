package mx.com.gourmeet.app.data.models

data class CrearHogarRequest(
    val CLI_ID: Int,
    val HOG_NOMBRE: String,
    val HOG_ICONO: String,
    val HOG_LATITUD: Double?,
    val HOG_LONGITUD: Double?,
    val HOG_DIRECCION: String?
)