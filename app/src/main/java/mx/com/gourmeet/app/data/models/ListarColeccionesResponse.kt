package mx.com.gourmeet.app.data.models

data class ListarColeccionesResponse(

    val success: Boolean,

    val colecciones: List<Coleccion>,

    val message: String? = null

)
