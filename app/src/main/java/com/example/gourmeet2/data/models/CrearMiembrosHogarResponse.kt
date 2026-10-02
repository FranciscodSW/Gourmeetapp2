package com.example.gourmeet2.data.models

data class CrearMiembrosHogarResponse(
    val success: Boolean,
    val message: String,
    val HOG_ID: Int?,
    val total_miembros: Int?
)