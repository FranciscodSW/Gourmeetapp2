package mx.com.gourmeet.app.data.models

import com.google.gson.annotations.SerializedName

data class IntegranteHogarBusqueda(
    @SerializedName("HOG_USU_ID")
    val HOG_USU_ID: Int,

    @SerializedName("HOG_ID")
    val HOG_ID: Int,

    @SerializedName("HOG_USU_NOMBRE")
    val HOG_USU_NOMBRE: String,

    @SerializedName("HOG_USU_CORREO")
    val HOG_USU_CORREO: String?,

    @SerializedName("HOG_USU_ROL")
    val HOG_USU_ROL: String
)