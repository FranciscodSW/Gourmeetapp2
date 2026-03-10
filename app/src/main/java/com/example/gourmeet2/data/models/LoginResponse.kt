package com.example.gourmeet2.data.models

data class LoginResponse(
    val success: Boolean,
    val usuario_id: Int?,
    val nombre: String?,
    val correo: String?,
    val error: String?
)