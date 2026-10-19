package mx.com.gourmeet.app.data.models

data class ActualizarEstadoDiaRequest(
    val PLA_ID: Int,
    val HOG_ID: Int,
    val CLI_ID: Int,
    val PLD_FECHA: String,
    val PLD_COCINAR: Int
)