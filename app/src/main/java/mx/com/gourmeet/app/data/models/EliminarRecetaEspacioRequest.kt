package mx.com.gourmeet.app.data.models

data class EliminarRecetaEspacioRequest(
    val ECR_ID: Int,
    val ECM_ID: Int,
    val PLA_ID: Int,
    val CLI_ID: Int,
    val HOG_ID: Int
)