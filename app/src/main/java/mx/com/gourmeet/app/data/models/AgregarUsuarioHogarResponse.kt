package mx.com.gourmeet.app.data.models

data class AgregarUsuarioHogarResponse(
    val success: Boolean,
    val message: String,
    val HOG_USU_ID: Int?,
    val HOG_ID: Int?
)