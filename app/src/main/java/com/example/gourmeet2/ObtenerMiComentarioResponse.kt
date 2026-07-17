package com.example.gourmeet2.data.models

data class ObtenerMiComentarioResponse(

    val success: Boolean,

    val comentarioExiste: Boolean,

    val comentarioId: Int? = null,

    val comentario: String? = null,

    val calificacion: Double? = null

)