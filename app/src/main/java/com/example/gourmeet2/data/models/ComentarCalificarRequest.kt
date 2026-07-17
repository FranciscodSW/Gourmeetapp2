package com.example.gourmeet2.data.models

data class ComentarCalificarRequest(

    val CLI_ID: Int,

    val REC_ID: Int,

    val COMENTARIO: String,

    val CALIFICACION: Double

)