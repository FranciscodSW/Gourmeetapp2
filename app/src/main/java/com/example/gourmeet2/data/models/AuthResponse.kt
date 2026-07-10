package com.example.gourmeet2.data.models

data class AuthResponse(

    val success: Boolean,

    val message: String,

    val usuario: Usuario?
)