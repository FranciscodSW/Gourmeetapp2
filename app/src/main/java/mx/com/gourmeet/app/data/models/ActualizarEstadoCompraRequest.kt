package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class ActualizarEstadoCompraRequest(
    @SerializedName("LCI_ID")
    val lciId: Int,

    @SerializedName("LCI_COMPRADO")
    val comprado: Int,

    @SerializedName("CLI_ID")
    val cliId: Int
)