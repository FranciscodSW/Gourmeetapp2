package com.example.gourmeet2.data.models

data class Comentarios(
    val id: Int,
    val comentario: String,
    val estatus: Int,
    val fecha: String,
    val calificacion: Float,
    val esMio: Boolean,
    val usuario: UsuarioComentario,
    val respuestas: List<RespuestaComentario>,
    var likes: Int,
    var dislikes: Int,
    var reportes: Int,
    var miReaccion: String?
)