package mx.com.gourmeet.app.data.models

data class DetalleRecetaResponse(
    val success: Boolean,
    val receta: RecetaDetalle,
    val ingredientes: List<IngredienteDetalle>,
    val preparacion: List<PasoPreparacion>,
    val comentarios: List<ComentarioReceta>,
    val calificacion: CalificacionReceta
)