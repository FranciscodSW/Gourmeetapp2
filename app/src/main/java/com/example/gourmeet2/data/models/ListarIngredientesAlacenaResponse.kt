package com.example.gourmeet2.data.models
data class ListarIngredientesAlacenaResponse(
    val success: Boolean,
    val message: String,
    val alacena: Alacena?,
    val total: Int,
    val ingredientes: List<IngredienteAlacena>
)