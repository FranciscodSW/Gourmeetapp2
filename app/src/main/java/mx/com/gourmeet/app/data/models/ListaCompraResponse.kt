package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class ListaCompraResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("lista")
    val lista: DatosListaCompra?,

    @SerializedName("resumen")
    val resumen: ResumenListaCompra?
)

data class DatosListaCompra(
    @SerializedName("LCO_ID")
    val lcoId: Int,

    @SerializedName("PLA_ID")
    val plaId: Int,

    @SerializedName("HOG_ID")
    val hogId: Int?,

    @SerializedName("LCO_FECHA_CREACION")
    val fechaCreacion: String?,

    @SerializedName("LCO_ESTADO")
    val estado: String?
)

data class ResumenListaCompra(
    @SerializedName("total_ingredientes")
    val totalIngredientes: Int,

    @SerializedName("ingredientes_comprados")
    val ingredientesComprados: Int,

    @SerializedName("ingredientes_pendientes")
    val ingredientesPendientes: Int
)