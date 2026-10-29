package mx.com.gourmeet.app.data.models

data class ListarIngredientesAlacenaRequest(
    val ALC_ID: Int,
    val ALC_CLI_ID: Int? = null,
    val HOG_ID: Int? = null
)