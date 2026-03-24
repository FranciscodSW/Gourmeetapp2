package com.example.gourmeet2.data.models

data class FacebookRegistroResponse(
    val success: Boolean,
    val usuario_id: Int?,
    val registro: Boolean,
    val login: Boolean? = null, // 🔥 agrégalo como en Google
    val nombre: String? = null,
    val error: String? = null
)