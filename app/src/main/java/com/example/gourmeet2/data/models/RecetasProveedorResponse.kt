package com.example.gourmeet2.data.models

data class RecetasProveedorResponse(
    val success: Boolean,
    val idProveedor: Int?,
    val total: Int?,
    val recetas: List<RecetaconFiltro>?
)