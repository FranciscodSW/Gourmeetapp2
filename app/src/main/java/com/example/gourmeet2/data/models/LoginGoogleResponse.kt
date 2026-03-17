package com.example.gourmeet2.data.models

data class LoginGoogleResponse(
    val success: Boolean,
    val usuario_id: Int?,
    val nombre: String?,
    val correo: String?,
    val error: String?
)