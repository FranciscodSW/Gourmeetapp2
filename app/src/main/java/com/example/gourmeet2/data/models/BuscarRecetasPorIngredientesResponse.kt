package com.example.gourmeet2.data.models

data class BuscarRecetasPorIngredientesResponse(

    val success: Boolean,

    val recetas: List<RecetaconFiltro> = emptyList()

)