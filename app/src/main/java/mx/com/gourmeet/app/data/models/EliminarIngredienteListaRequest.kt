package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class EliminarIngredienteListaRequest(

    @SerializedName("LCI_ID")
    val lciId: Int
)