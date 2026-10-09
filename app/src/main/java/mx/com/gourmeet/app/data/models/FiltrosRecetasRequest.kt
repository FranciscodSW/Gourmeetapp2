package mx.com.gourmeet.app.data.models

data class FiltrosRecetasRequest(
    val ingredientes: List<Int>,
    val categoriaId: Int? = null
)