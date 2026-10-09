package mx.com.gourmeet.app.data.models

data class BuscarIngredientesResponse(
    val success: Boolean,
    val count: Int,
    val ingredientes: List<BuscarIngredientes>
)

