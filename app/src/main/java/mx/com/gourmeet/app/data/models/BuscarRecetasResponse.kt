package mx.com.gourmeet.app.data.models
data class BuscarRecetasResponse(
    val success: Boolean,
    val count: Int,
    val recetas: List<BuscarRecetas>
)