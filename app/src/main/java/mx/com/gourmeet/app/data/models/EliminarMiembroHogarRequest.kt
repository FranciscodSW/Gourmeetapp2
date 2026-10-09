package mx.com.gourmeet.app.data.models

data class EliminarMiembroHogarRequest(
    val HOG_ID: Int,
    val CLI_ID_PROPIETARIO: Int,
    val HOG_USU_ID: Int
)