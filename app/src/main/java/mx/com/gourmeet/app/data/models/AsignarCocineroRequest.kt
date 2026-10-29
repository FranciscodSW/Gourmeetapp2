package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class AsignarCocineroRequest(
    @SerializedName("PLD_ID")
    val pldId: Int,

    @SerializedName("HOG_USU_ID_COCINERO")
    val hogUsuIdCocinero: Int,

    @SerializedName("HOG_USU_ID_ASIGNO")
    val hogUsuIdAsigno: Int
)
