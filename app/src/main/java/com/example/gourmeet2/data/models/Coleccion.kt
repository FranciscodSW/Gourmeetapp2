package com.example.gourmeet2.data.models

data class Coleccion(

    val id: Int,

    val nombre: String,

    val portada: String?,

    val privada: Boolean,
    val recetas: List<RecetaSimple> = emptyList()

)