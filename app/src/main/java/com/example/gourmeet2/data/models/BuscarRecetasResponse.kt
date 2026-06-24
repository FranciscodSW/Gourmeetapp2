package com.example.gourmeet2.data.models
data class BuscarRecetasResponse(
    val success: Boolean,
    val count: Int,
    val recetas: List<BuscarRecetas>
)