package mx.com.gourmeet.app.data.models

data class BuscarIngredientes(
    val id: Int,
    val nombre: String,
    val imagen_url: String? = null,
    val categoria: String? = null,
    val familia: String? = null
)