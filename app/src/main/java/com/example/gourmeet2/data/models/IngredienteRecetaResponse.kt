package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class IngredienteRecetaResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("receta") val receta: RecetaConIngredientes?,
    @SerializedName("comentarios") val comentarios: List<Comentario>?,
    @SerializedName("error") val error: String?
)