package mx.com.gourmeet.app.data.models

data class FiltrosRecetasNombreRequest(
    val busqueda: String,
    val categoriaId: Int? = null

)