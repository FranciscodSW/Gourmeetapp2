package com.example.gourmeet2.data.models
import com.google.gson.annotations.SerializedName
data class BuscarIngredientes(
    val id: Int,
    val nombre: String,
    val imagen_url: String? = null
)