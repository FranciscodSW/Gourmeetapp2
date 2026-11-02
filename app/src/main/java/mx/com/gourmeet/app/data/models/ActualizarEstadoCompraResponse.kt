package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class ActualizarEstadoCompraResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("ingrediente")
    val ingrediente: EstadoIngredienteCompra?
)

data class EstadoIngredienteCompra(

    @SerializedName("LCI_ID")
    val lciId: Int,

    @SerializedName("LCO_ID")
    val lcoId: Int,

    @SerializedName("ING_ID")
    val ingId: Int,

    @SerializedName("LCI_COMPRADO")
    val comprado: Int
)