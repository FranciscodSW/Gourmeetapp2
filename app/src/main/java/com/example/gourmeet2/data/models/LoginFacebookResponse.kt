package com.example.gourmeet2.data.models

data class LoginFacebookResponse(
    val success: Boolean,
    val usuario_id: Int?,
    val nombre: String?,
    val correo: String?,
    val login: Boolean? = null,
    val error: String? = null
)