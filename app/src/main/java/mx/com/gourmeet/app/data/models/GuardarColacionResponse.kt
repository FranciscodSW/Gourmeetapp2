package mx.com.gourmeet.app.data.models

data class GuardarColacionResponse(
    val success: Boolean,
    val message: String,
    val data: ColacionGuardadaData? = null
)
