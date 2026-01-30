package com.example.gourmeet2.data.models

data class CategoriaResponse(

    val success: Boolean,
    val count: Int,
    val categorias: List<Categoria>
)