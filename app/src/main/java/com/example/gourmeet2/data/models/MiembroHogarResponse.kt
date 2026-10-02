package com.example.gourmeet2.data.models

data class MiembroHogarResponse(
    val success: Boolean,
    val mensaje: String,
    val miembros: List<MiembroHogar>
)

data class MiembroHogar(
    val HOG_USU_ID: Int,
    val HOG_ID: Int,
    val CLI_ID: Int?,
    val HOG_USU_NOMBRE: String?,
    val HOG_USU_CORREO: String?,
    val HOG_USU_ESTADO: String,
    val HOG_USU_ROL: String
)