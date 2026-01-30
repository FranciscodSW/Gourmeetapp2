package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class RecetaRecrcidResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("categoria_id") val categoriaId: Int? = null, // Cambia a nullable
    @SerializedName("count") val count: Int,
    @SerializedName("recetas") val recetas: List<RecetaRecrcid>
)