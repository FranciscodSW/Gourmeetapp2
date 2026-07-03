package com.example.gourmeet2.data.models

data class ComentarioReceta(
    val COM_ID: Int,
    val usuario: String,
    val foto: String?,
    val comentario: String,
    val fecha: String,
    val likes: Int,
    val dislikes: Int,
    val respuesta: RespuestaComentario?
)