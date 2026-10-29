package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class AsignarCocineroResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("PCC_ID")
    val pccId: Int? = null,

    @SerializedName("PLD_ID")
    val pldId: Int? = null,

    @SerializedName("PLA_ID")
    val plaId: Int? = null,

    @SerializedName("HOG_ID")
    val hogId: Int? = null,

    @SerializedName("PLD_FECHA")
    val pldFecha: String? = null,

    @SerializedName("HOG_USU_ID_COCINERO")
    val hogUsuIdCocinero: Int? = null,

    @SerializedName("HOG_USU_NOMBRE")
    val hogUsuNombre: String? = null,

    @SerializedName("HOG_USU_ID_ASIGNO")
    val hogUsuIdAsigno: Int? = null,

    @SerializedName("PCC_ESTADO")
    val pccEstado: String? = null
)