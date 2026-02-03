// RecetaRecrcid.kt
package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class RecetaRecrcid(
    @SerializedName("REC_ID") val id: Int,
    @SerializedName("REC_NOMBRE") val nombre: String?,
    @SerializedName("REC_DESCRIPCION") val descripcion: String?,
    @SerializedName("REC_TIEMPO_PREPARACION") val tiempoPreparacion: String?,
    @SerializedName("REC_PORCIONES") val porciones: Int,
    @SerializedName("REC_FECHACREACION") val fechaCreacion: String?, // ¡CON "E"!
    @SerializedName("Dificultad") val dificultad: String?, // Nullable
    @SerializedName("Calorias") val calorias: String?, // String, no Float (viene vacío "")
    @SerializedName("REC_ENLACEYOUTUBE") val enlaceYoutube: String?, // Nullable
    @SerializedName("REC_RC_ID") val recrcid: Int
)
