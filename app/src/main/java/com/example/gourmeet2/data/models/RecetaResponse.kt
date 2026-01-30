package com.example.gourmeet2.data.models

data class RecetaResponse(
    val success: Boolean,
    val pagina_actual: Int,
    val total_paginas: Int,
    val total_recetas: Int,
    val limite_por_pagina: Int,
    val recetas: List<Receta>
)