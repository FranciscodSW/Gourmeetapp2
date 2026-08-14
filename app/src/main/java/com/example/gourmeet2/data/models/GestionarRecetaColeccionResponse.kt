package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class GestionarRecetaColeccionResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String

)