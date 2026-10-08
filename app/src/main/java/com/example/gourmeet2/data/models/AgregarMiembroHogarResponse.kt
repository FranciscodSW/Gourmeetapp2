package com.example.gourmeet2.data.models

data class AgregarMiembroHogarResponse(
    val success: Boolean,
    val message: String,
    val HOG_USU_ID: Int? = null,
    val HOG_ID: Int? = null,
    val CLI_ID: Int? = null,
    val HOG_USU_ESTADO: String? = null,
    val HOG_USU_ROL: String? = null
)