package mx.com.gourmeet.app.data.models

data class RecetasProveedorResponse(
    val success: Boolean,
    val idProveedor: Int?,
    val total: Int?,
    val recetas: List<RecetaconFiltro>?
)