package com.example.gourmeet2.data.models

data class RespuestaComentarioRequest(

    val accion: String,

    val receta: Int = 0,

    val usuario: Int,

    val comentarioPadre: Int = 0,

    val respuesta: Int = 0,

    val comentario: String = ""

)