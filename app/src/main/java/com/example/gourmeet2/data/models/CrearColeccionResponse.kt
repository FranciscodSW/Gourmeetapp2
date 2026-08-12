package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class CrearColeccionResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("COL_ID")
    val COL_ID: Int?,

    @SerializedName("message")
    val message: String

)