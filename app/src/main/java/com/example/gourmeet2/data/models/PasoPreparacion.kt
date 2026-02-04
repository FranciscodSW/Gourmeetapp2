package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class PasoPreparacion(
    @SerializedName("PRE_PASO") val paso: Int,
    @SerializedName("PRE_DESCRIPCION") val descripcion: String,
    @SerializedName("PRE_Tiempo") val  tiempo: Float? = null
)