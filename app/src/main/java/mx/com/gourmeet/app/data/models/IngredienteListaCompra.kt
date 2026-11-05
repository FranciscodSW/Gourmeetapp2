package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class IngredienteListaCompra(

    @SerializedName("LCI_ID")
    val lciId: Int,

    @SerializedName("LCO_ID")
    val lcoId: Int,

    @SerializedName("ING_ID")
    val ingId: Int,

    @SerializedName("ING_DESCRIPCION")
    val descripcion: String,

    @SerializedName("Foto_Ingrediente")
    val fotoIngrediente: String?,

    @SerializedName("categoria")
    val categoria: String?,

    @SerializedName("familia")
    val familia: String?,

    @SerializedName("tipo")
    val tipo: String?,

    @SerializedName("LCI_CANTIDAD")
    val cantidad: Double?,

    @SerializedName("LCI_UNIDAD")
    val unidad: String?,

    @SerializedName("LCI_COMPRADO")
    val comprado: Int,

    @SerializedName("LCI_FECHA_AGREGADO")
    val fechaAgregado: String?
)