package com.example.gourmeet2.data.models

data class FacebookRegistro(
    val correo: String?,
    val nombre: String,
    val facebook_id: String,
    val avatar: String,
    val edad: Int = 0,
    val nivel: Int = 1,
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val restricciones: List<Int> = emptyList()
)