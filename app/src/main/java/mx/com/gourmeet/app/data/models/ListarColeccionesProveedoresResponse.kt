package mx.com.gourmeet.app.data.models

data class ListarColeccionesProveedoresResponse(
    val success: Boolean,
    val CLI_ID: Int,
    val total_colecciones: Int,
    val colecciones: List<ColeccionProveedor>?,
    val mensaje: String?
)