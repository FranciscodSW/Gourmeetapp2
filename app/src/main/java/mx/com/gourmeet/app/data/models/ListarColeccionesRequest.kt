package mx.com.gourmeet.app.data.models

data class ListarColeccionesRequest(

    val cliente: Int,
    val limite: Int = 3

)