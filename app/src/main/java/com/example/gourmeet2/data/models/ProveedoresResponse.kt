package com.example.gourmeet2.data.models

data class ProveedoresResponse(
    val success: Boolean,

    val proveedores: List<Proveedor>,

    val categorias: List<String>,

    val total_proveedores: Int,

    val total_categorias: Int
)