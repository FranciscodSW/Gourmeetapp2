package com.example.gourmeet2.data.models

data class ListarColeccionesResponse(

    val success: Boolean,

    val colecciones: List<Coleccion>,

    val message: String? = null

)
