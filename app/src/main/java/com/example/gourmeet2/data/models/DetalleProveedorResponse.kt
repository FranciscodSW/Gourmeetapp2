package com.example.gourmeet2.data.models

data class DetalleProveedorResponse(

    val success: Boolean,

    val proveedor: Proveedor?,

    val mensaje: String? = null
)