package com.example.gourmeet2.data.models

data class FiltrosRecetasResponse(
    val success: Boolean,
    val coincidencia: List<RecetaconFiltro>,
    val calorias: List<RecetaconFiltro>,
    val tiempo: List<RecetaconFiltro>,
    val gasto: List<RecetaconFiltro>,
    val sin_lacteos: List<RecetaconFiltro>,
    val sin_azucar: List<RecetaconFiltro>,
    val dificultad: List<RecetaconFiltro>
)