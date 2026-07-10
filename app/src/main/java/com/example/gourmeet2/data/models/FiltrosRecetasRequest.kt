package com.example.gourmeet2.data.models

data class FiltrosRecetasRequest(
    val ingredientes: List<Int>,
    val categoriaId: Int? = null
)