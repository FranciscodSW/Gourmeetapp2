package mx.com.gourmeet.app.data.models

data class ObtenerAlacenaHogarResponse(
    val success: Boolean,
    val message: String,
    val alacena: Alacena?
)