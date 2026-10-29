package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class BuscarIntegrantesHogarRequest(
    @SerializedName("HOG_ID")
    val HOG_ID: Int,

    @SerializedName("HOG_USU_ID_SOLICITANTE")
    val HOG_USU_ID_SOLICITANTE: Int,

    @SerializedName("busqueda")
    val busqueda: String
)