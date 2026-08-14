package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class GestionarFavoritoRequest(

    @SerializedName("CLI_ID")
    val CLI_ID: Int,

    @SerializedName("REC_ID")
    val REC_ID: Int

)