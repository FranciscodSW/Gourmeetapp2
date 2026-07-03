package com.example.gourmeet2.data.models

data class DetalleRecetaResponse(
    val success: Boolean,
    val receta: RecetaDetalle,
    val ingredientes: List<IngredienteDetalle>,
    val preparacion: List<PasoPreparacion>,
    val comentarios: List<ComentarioReceta>,
    val calificacion: CalificacionReceta
)