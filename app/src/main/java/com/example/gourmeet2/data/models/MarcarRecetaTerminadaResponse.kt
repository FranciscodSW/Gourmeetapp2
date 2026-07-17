package com.example.gourmeet2.data.models

data class MarcarRecetaTerminadaResponse(

    val success: Boolean,

    val mensaje: String,

    val xpAnterior: Int,

    val xpGanada: Int,

    val xpActual: Int,

    val nivelAnterior: Int,

    val nivelNuevo: Int,

    val subioNivel: Boolean

)