package mx.com.gourmeet.app.data.models

data class IngredienteClasificacionResponse(
    val success: Boolean,
    val count: Int?,
    val message: String?,
    val ingredientes: List<IngredienteClasificacion>?
)