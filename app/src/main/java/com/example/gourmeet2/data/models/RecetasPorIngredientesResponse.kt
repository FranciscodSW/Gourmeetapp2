package com.example.gourmeet2.data.models
data class RecetasPorIngredientesResponse(
    val categoria: Int,
    val ingredientes: List<String>,
    val count: Int,
    val recetas: List<RecetasPorIngredientes>
)