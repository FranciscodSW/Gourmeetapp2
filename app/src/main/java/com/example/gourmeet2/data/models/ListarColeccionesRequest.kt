package com.example.gourmeet2.data.models

data class ListarColeccionesRequest(

    val cliente: Int,
    val limite: Int = 3

)