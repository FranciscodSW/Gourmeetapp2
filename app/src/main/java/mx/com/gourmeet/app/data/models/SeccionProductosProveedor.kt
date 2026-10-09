package mx.com.gourmeet.app.data.models

data class SeccionProductosProveedor(

    val titulo: String,

    val ingredientes: List<IngredienteProveedor> =
        emptyList(),

    val recetas: List<RecetaconFiltro> =
        emptyList(),

    val esRecetas: Boolean = false,

    val nombreColeccion: String? = null
)