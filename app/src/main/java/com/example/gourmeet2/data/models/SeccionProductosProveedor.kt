package com.example.gourmeet2.data.models

data class SeccionProductosProveedor(

    val titulo: String,

    val ingredientes: List<IngredienteProveedor> =
        emptyList(),

    val recetas: List<RecetaconFiltro> =
        emptyList(),

    val esRecetas: Boolean = false,

    val nombreColeccion: String? = null
)