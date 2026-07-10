package com.example.gourmeet2.data.models
data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String,
    val foto: String?,
    val nivel: Int?,
    val edad: Int?,
    val origen: String?
)