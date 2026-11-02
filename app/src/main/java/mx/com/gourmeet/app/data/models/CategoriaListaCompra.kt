package mx.com.gourmeet.app.data.models


data class CategoriaListaCompra(
    val nombreCategoria: String,
    val ingredientes: List<IngredienteListaCompra>
)