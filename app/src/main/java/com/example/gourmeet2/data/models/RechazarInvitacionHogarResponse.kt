package com.example.gourmeet2.data.models

data class RechazarInvitacionHogarResponse(
    val success: Boolean,
    val message: String,
    val NOT_ID: Int? = null,
    val HOG_USU_ID: Int? = null,
    val HOG_ID: Int? = null,
    val CLI_ID: Int? = null,
    val NOT_ESTADO: String? = null,
    val HOG_USU_ESTADO: String? = null,
    val NOT_LEIDA: Int? = null
)