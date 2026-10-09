package mx.com.gourmeet.app.data.models

data class DetalleProveedorResponse(

    val success: Boolean,

    val proveedor: Proveedor?,

    val mensaje: String? = null
)