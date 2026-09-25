package com.example.gourmeet2.data.models

data class Hogar(
    val HOG_ID: Int,
    val HOG_NOMBRE: String,
    val HOG_ICONO: String,
    val HOG_LATITUD: Double?,
    val HOG_LONGITUD: Double?,
    val HOG_DIRECCION: String?,
    val HOG_FECHA_CREACION: String?,
    val HOG_ESTATUS: Int
)