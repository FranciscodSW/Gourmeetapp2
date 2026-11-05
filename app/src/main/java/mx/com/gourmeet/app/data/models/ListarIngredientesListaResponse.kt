package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class ListarIngredientesListaResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("lista")
    val lista: DatosListaCompra?,

    @SerializedName("total_ingredientes")
    val totalIngredientes: Int?,

    @SerializedName("ingredientes")
    val ingredientes: List<IngredienteListaCompra> = emptyList()
)