package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class VerificarFavoritoResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("guardada")
    val guardada: Boolean,

    @SerializedName("COL_ID")
    val COL_ID: Int?,

    @SerializedName("REC_ID")
    val REC_ID: Int?,

    @SerializedName("message")
    val message: String

)