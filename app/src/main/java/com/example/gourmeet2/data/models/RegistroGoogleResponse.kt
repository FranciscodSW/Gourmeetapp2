package com.example.gourmeet2.data.models

data class RegistroGoogleResponse(
    val success: Boolean,
    val usuario_id: Int?,
    val nombre: String?,
    val correo: String?,
    val login: Boolean?,
    val registro: Boolean?,
    val error: String?
)