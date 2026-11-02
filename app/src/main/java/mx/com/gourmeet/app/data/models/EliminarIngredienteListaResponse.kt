package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class EliminarIngredienteListaResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("ingrediente")
    val ingrediente: IngredienteEliminado?
)

data class IngredienteEliminado(

    @SerializedName("LCI_ID")
    val lciId: Int,

    @SerializedName("LCO_ID")
    val lcoId: Int,

    @SerializedName("ING_ID")
    val ingId: Int
)