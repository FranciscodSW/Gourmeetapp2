package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class PasosResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int,
    @SerializedName("pasos") val pasos: List<PasoPreparacion>
)