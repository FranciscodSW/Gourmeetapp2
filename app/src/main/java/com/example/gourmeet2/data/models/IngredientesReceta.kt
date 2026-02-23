package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

// Modelo para ingrediente individual
data class Ingrediente(
    @SerializedName("RI_ID") val riId: Int,
    @SerializedName("RI_DESC_CANTIDAD") val cantidad: String,
    @SerializedName("Nombre_Ingrediente") val nombreIngrediente: String,
    @SerializedName("Calorias_Ingrediente") val caloriasIngrediente: Int,
    @SerializedName("Precio_Estimado") val precioEstimado: Double
)

// Modelo para receta con ingredientes
data class RecetaConIngredientes(
    @SerializedName("REC_ID") val recId: Int,
    @SerializedName("REC_NOMBRE") val recNombre: String,
    @SerializedName("REC_DESCRIPCION") val recDescripcion: String,
    @SerializedName("REC_TIEMPO_PREPARACION") val recTiempoPreparacion: String,
    @SerializedName("REC_PORCIONES") val recPorciones: Int,
    @SerializedName("REC_FECHACREACION") val recFechaCreacion: String,
    @SerializedName("Dificultad") val recDificultad: String?,
    @SerializedName("Calorias") val recCalorias: Float?,
    @SerializedName("REC_ENLACEYOUTUBE") val recEnlaceYoutube: String?,
    @SerializedName("REC_RC_ID") val recCategoriaId: Int,
    @SerializedName("FotoReceta") val fotoReceta: String?,
    @SerializedName("promedio") val promedio: Float,
    @SerializedName("votos") val votos: Int,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("ingredientes") val ingredientes: List<Ingrediente>
)

