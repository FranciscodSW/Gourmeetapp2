package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class ActualizarColeccionRequest(

    @SerializedName("COL_ID")
    val COL_ID: Int,

    @SerializedName("CLI_ID")
    val CLI_ID: Int,

    @SerializedName("COL_NOMBRE")
    val COL_NOMBRE: String,

    @SerializedName("COL_PORTADA")
    val COL_PORTADA: String,

    @SerializedName("COL_PRIVADA")
    val COL_PRIVADA: Int,

    @SerializedName("RECETAS")
    val RECETAS: List<Int>

)