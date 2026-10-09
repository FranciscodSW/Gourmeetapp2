package mx.com.gourmeet.app.data.models

data class GuardarIngredienteAlacenaResponse(
    val success: Boolean,
    val message: String,
    val ALC_ID: Int?,
    val ING_ID: Int?
)