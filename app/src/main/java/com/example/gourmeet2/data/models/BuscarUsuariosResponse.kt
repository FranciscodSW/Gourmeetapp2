package com.example.gourmeet2.data.models

data class BuscarUsuariosResponse(
    val success: Boolean,
    val mensaje: String,
    val usuarios: List<UsuarioBusqueda>
)