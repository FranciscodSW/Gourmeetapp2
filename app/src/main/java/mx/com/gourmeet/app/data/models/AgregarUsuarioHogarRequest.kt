package mx.com.gourmeet.app.data.models

data class AgregarUsuarioHogarRequest(
    val HOG_ID: Int,
    val CLI_ID: Int?,
    val HOG_USU_NOMBRE: String?,
    val HOG_USU_CORREO: String?,
    val HOG_USU_ESTADO: String,
    val HOG_USU_ROL: String
)