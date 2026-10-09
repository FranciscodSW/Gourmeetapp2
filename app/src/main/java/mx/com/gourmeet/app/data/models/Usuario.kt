package mx.com.gourmeet.app.data.models
data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String,
    val foto: String?,
    val nivel: Int?,
    val edad: Int?,
    val puntos: Int?,
    val origen: String?
)