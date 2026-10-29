package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class DiaCocinaResponse(
    @SerializedName("PLD_ID")
    val pldId: Int,

    @SerializedName("PLA_ID")
    val plaId: Int,

    @SerializedName("PLD_FECHA")
    val pldFecha: String,

    @SerializedName("PLD_COCINAR")
    val pldCocinar: Int,

    @SerializedName("HOG_USU_ID_COCINERO")
    val hogUsuIdCocinero: Int? = null,

    @SerializedName("HOG_USU_NOMBRE")
    val hogUsuNombre: String? = null
)