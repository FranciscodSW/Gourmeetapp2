package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class VerificarFavoritoRequest(

    @SerializedName("CLI_ID")
    val CLI_ID: Int,

    @SerializedName("REC_ID")
    val REC_ID: Int

)