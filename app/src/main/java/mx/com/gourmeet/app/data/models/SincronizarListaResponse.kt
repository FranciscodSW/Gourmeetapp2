package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class SincronizarListaResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("LCO_ID")
    val lcoId: Int?,

    @SerializedName("PLA_ID")
    val plaId: Int?,

    @SerializedName("HOG_ID")
    val hogId: Int?,

    @SerializedName("MODALIDAD")
    val modalidad: String? = null,

    @SerializedName("total_ingredientes")
    val totalIngredientes: Int?,

    @SerializedName("ingredientes_comprados")
    val ingredientesComprados: Int?,

    @SerializedName("ingredientes_pendientes")
    val ingredientesPendientes: Int?,

    @SerializedName("ingredientes_al_gusto")
    val ingredientesAlGusto: Int?,

    @SerializedName("ingredientes_no_interpretables")
    val ingredientesNoInterpretables: Int?
)