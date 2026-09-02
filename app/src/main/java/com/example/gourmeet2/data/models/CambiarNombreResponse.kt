package com.example.gourmeet2.data.models

data class CambiarNombreResponse(

    val success: Boolean,

    val mensaje: String?,

    val CLI_ID: Int?,

    val CLI_NOMBRE: String?,

    val actualizado: Boolean?

)