package mx.com.gourmeet.app.data.models

data class CrearColeccionProveedorResponse (
    val success: Boolean,

    val mensaje: String?,

    val guardado: Boolean,

    val coleccionId: Int?

)