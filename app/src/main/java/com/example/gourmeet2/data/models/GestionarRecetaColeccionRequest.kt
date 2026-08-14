package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class GestionarRecetaColeccionRequest(

    @SerializedName("CLI_ID")
    val CLI_ID: Int,

    @SerializedName("COL_ID")
    val COL_ID: Int,

    @SerializedName("REC_ID")
    val REC_ID: Int,

    @SerializedName("ACCION")
    val ACCION: String

)