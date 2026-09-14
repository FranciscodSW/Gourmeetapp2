package com.example.gourmeet2.data.models

data class IngredientesAlacenaResponse(
    val success: Boolean,
    val message: String?,
    val alacena: AlacenaInfo?,
    val total: Int?,
    val ingredientes: List<IngredienteAlacena>?
)

data class AlacenaInfo(
    val ALC_ID: Int,
    val ALC_CLI_ID: Int,
    val ALC_NOMBRE: String,
    val ALC_ICONO: String?
)