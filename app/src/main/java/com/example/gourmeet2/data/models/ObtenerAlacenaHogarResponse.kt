package com.example.gourmeet2.data.models

data class ObtenerAlacenaHogarResponse(
    val success: Boolean,
    val message: String,
    val alacena: Alacena?
)