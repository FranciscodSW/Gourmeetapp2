package mx.com.gourmeet.app.data.models

data class BuscarRecetasPorIngredientesResponse(

    val success: Boolean,

    val recetas: List<RecetaconFiltro> = emptyList()

)