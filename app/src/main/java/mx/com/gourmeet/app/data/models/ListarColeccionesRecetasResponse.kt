package mx.com.gourmeet.app.data.models

data class ListarColeccionesRecetasResponse(

    val success: Boolean,

    val colecciones: List<ColeccionConRecetas>

)