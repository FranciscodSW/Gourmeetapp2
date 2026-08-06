package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class ReportarRecetaResponse(

    @SerializedName("success")
    val success:Boolean,

    @SerializedName("mensaje")
    val mensaje:String

)