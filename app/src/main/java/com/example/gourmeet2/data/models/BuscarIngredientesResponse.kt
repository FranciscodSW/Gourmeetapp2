package com.example.gourmeet2.data.models

data class BuscarIngredientesResponse (
    val success: Boolean,
    val count: Int,
    val ingredientes: List<BuscarIngredientes>
)

