package mx.com.gourmeet.app.data.models

data class RegistroGoogle(
    val correo: String,
    val nombre: String,
    val google_id: String,
    val avatar: String,
    val edad: Int,
    val nivel: Int = 0,
    val latitud: Double,
    val longitud: Double
)