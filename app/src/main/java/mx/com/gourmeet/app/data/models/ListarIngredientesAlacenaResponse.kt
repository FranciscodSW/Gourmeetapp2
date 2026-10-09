package mx.com.gourmeet.app.data.models
data class ListarIngredientesAlacenaResponse(
    val success: Boolean,
    val message: String,
    val alacena: Alacena?,
    val total: Int,
    val ingredientes: List<IngredienteAlacena>
)