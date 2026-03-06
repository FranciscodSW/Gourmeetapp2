package com.example.gourmeet2.data.models


data class RestriccionesResponse(

    val alergia: List<Restriccion>,
    val alimento: List<Restriccion>,
    val cultural: List<Restriccion>,
    val intolerancia: List<Restriccion>

)