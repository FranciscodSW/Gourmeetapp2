package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class RecetasPorIngredientes(

    @SerializedName("REC_ID") val id: Int,
    @SerializedName("REC_NOMBRE") val nombre: String,
    @SerializedName("REC_DESCRIPCION") val descripcion: String,
    @SerializedName("REC_TIEMPO_PREPARACION") val tiempoPreparacion: String,
    @SerializedName("REC_PORCIONES") val porciones: String,
    @SerializedName("REC_FECHACREACION") val fechaCreacion: String,
    @SerializedName("Dificultad") val dificultad: String,
    @SerializedName("Calorias") val calorias: String,
    @SerializedName("REC_ENLACEYOUTUBE") val youtube: String?,
    @SerializedName("REC_RC_ID") val categoriaId: Int
)
