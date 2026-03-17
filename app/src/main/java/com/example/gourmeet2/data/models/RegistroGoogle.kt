package com.example.gourmeet2.data.models

data class RegistroGoogle(
    val correo: String,
    val nombre: String,
    val google_id: String,
    val avatar: String,
    val edad: Int,
    val nivel: Int,
    val latitud: Double,
    val longitud: Double,
    val restricciones: List<Int>
)