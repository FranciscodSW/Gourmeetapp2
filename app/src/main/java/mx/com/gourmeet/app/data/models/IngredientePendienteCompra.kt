package mx.com.gourmeet.app.data.models

data class IngredientePendienteCompra(
    val ingId: Int,
    val nombre: String,
    val foto: String?,
    val categoria: String?,
    val cantidad: Double?,
    val unidad: String?,
    var tachadoVisualmente: Boolean = false,
    var agregadoAlacena: Boolean = false
)