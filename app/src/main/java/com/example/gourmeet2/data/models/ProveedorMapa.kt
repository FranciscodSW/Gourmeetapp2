package com.example.gourmeet2.data.models

import java.io.Serializable

data class ProveedorMapa(
    val id: String,
    val nombre: String,
    val latitud: Double,
    val longitud: Double,
    val fotoPerfil: String?
) : Serializable