package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class BuscarIntegrantesHogarResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("total")
    val total: Int? = 0,

    @SerializedName("integrantes")
    val integrantes: List<IntegranteHogarBusqueda> = emptyList()
)