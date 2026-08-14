package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class EliminarColeccionRequest(

    @SerializedName("COL_ID")
    val COL_ID: Int,

    @SerializedName("CLI_ID")
    val CLI_ID: Int

)