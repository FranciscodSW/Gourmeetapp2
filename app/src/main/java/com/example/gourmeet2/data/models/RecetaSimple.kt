package com.example.gourmeet2.data.models

data class RecetaSimple(

    val REC_ID: Int,

    val REC_NOMBRE: String,

    val coincidencias: Int? = null,

    val calorias: Double? = null,

    val tiempo: String? = null,

    val gasto: Double? = null

)