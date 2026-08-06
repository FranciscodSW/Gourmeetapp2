package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class ReportarRecetaRequest(

    @SerializedName("CLI_ID")
    val cliId:Int,

    @SerializedName("REC_ID")
    val recId:Int,

    @SerializedName("PROBLEMA")
    val problema:String,

    @SerializedName("DESCRIPCION")
    val descripcion:String

)