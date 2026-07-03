package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class RecetaconFiltro(

    @SerializedName("REC_ID")
    val REC_ID: Int,

    @SerializedName("REC_NOMBRE")
    val REC_NOMBRE: String,

    @SerializedName("REC_TIEMPO_PREPARACION")
    val REC_TIEMPO_PREPARACION: String?,

    @SerializedName("Dificultad")
    val Dificultad: String?,

    @SerializedName("Categoria")
    val Categoria: String?,

    @SerializedName("FotoReceta")
    val FotoReceta: String?,

    @SerializedName("Ingredientes")
    val Ingredientes: List<IngredienteReceta> = emptyList(),

    @SerializedName("coincidencias")
    val coincidencias: Int? = null,

    @SerializedName("calorias")
    val calorias: Double? = null,

    @SerializedName("gasto")
    val gasto: Double? = null

)