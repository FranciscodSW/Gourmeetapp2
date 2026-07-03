package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class PasoPreparacion(
    val paso: Int,
    val descripcion: String,
    val tiempo: Float?
)