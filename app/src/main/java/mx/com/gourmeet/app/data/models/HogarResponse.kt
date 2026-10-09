package mx.com.gourmeet.app.data.models

data class HogarResponse(
    val success: Boolean,
    val tiene_hogar: Boolean,
    val mensaje: String,
    val hogar: Hogar?
)