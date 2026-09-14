package com.example.gourmeet2.data.models

data class ActualizarEstadosAlacenaResponse(
    val success: Boolean,
    val message: String?,
    val ALC_ID: Int?,
    val procesados: Int?,
    val actualizados: Int?,
    val sin_cambios: Int?,
    val no_aplican: Int?,
    val detalle: List<DetalleEstadoIngrediente>?
)

data class DetalleEstadoIngrediente(
    val ING_ID: Int?,
    val ingrediente: String?,
    val categoria: String?,
    val estado_anterior: String?,
    val estado_nuevo: String?,
    val estado: String?,
    val dias_transcurridos: Int?,
    val actualizado: Boolean?,
    val motivo: String?
)