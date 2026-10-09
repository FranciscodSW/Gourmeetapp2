package mx.com.gourmeet.app.data.models

import java.io.Serializable

data class ProveedorMapa(
    val id: String,
    val nombre: String,
    val latitud: Double,
    val longitud: Double,
    val fotoPerfil: String?
) : Serializable