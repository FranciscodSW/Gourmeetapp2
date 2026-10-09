package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class RecetasRealizadasRequest(
    @SerializedName("CLI_ID")
    val CLI_ID: Int
)