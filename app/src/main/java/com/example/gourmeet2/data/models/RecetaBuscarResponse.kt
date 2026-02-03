package com.example.gourmeet2.data.models

data class RecetaBuscarResponse (

    val success: Boolean,
    val q: String,
    val count: Int,
    val recetas: List<RecetaBuscar> ,
    val error: String? = null
)