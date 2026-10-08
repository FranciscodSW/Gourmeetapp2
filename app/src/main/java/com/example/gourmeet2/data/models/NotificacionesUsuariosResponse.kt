package com.example.gourmeet2.data.models

data class NotificacionesUsuariosResponse(
    val success: Boolean,
    val message: String,
    val total: Int,
    val notificaciones: List<NotificacionUsuario>
)
data class NotificacionUsuario(
    val NOT_ID: Int,
    val CLI_ID: Int,
    val HOG_USU_ID: Int?,
    val NOT_TIPO: String,
    val NOT_TITULO: String,
    val NOT_MENSAJE: String,
    val NOT_ESTADO: String,
    val NOT_LEIDA: Int,
    val NOT_FECHA: String
)