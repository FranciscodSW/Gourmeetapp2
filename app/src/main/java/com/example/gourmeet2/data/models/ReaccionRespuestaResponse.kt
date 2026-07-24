package com.example.gourmeet2.data.models

data class ReaccionRespuestaResponse(

    val success: Boolean,

    val respuesta: Int,

    val miReaccion: String?,

    val likes: Int,

    val dislikes: Int,

    val reportes: Int,

    val message: String?

)