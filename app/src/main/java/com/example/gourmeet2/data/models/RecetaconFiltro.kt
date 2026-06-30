package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class RecetaconFiltro(

    @SerializedName("REC_ID")
    val REC_ID: Int,

    @SerializedName("REC_NOMBRE")
    val REC_NOMBRE: String,

    @SerializedName("coincidencias")
    val coincidencias: Int? = null,

    @SerializedName("calorias")
    val calorias: Double? = null,

    @SerializedName("tiempo")
    val tiempo: String? = null,

    @SerializedName("gasto")
    val gasto: Double? = null,

    @SerializedName("FotoReceta")
    val FotoReceta: String? = null
)