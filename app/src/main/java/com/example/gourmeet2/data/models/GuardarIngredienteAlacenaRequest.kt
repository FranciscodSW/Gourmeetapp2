package com.example.gourmeet2.data.models

data class GuardarIngredienteAlacenaRequest(
    val ALC_ID: Int,
    val ING_ID: Int,
    val AI_CANTIDAD: Double,
    val AI_UNIDAD: String,
    val AI_FECHA_COMPRA: String?,
    val AI_FECHA_VENCIMIENTO: String?,
    val AI_ESTADO: String?,
    val AI_PRECIO_COMPRA: Double?,
    val AI_ALMACENAMIENTO: String?,
    val AI_FRECUENCIA_CONSUMO: String?,
    val AI_TIPO_ABASTECIMIENTO: String?
)