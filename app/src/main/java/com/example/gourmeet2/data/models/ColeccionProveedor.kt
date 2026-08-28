package com.example.gourmeet2.data.models

data class ColeccionProveedor(
    val COP_ID: Int,
    val COP_NOMBRE: String?,
    val COP_PORTADA: String?,
    val COP_PRIVADA: Int,
    val COP_FECHA: String?,
    val proveedores: List<Proveedor>
)