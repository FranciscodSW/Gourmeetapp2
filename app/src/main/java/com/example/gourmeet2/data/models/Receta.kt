package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class Receta(
    @SerializedName("REC_ID") val id: Int,
    @SerializedName("REC_NOMBRE") val nombre: String,
    @SerializedName("REC_DESCRIPCION") val descripcion: String,
    @SerializedName("REC_TIEMPO_PREPARACION") val tiempoPreparacion: String,
    @SerializedName("REC_PORCIONES") val porciones: Int,
    @SerializedName("REC_FECHACREACION") val fechaCreacion: String,
    @SerializedName("Dificultad") val dificultad: String?,
    @SerializedName("Calorias") val calorias: Float?,
    @SerializedName("REC_ENLACEYOUTUBE") val enlaceYoutube: String?
)