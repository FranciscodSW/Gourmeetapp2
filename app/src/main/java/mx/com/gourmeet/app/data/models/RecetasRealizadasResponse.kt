package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class RecetasRealizadasResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("recetas")
    val recetas: List<RecetaconFiltro> = emptyList()
)