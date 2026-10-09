package mx.com.gourmeet.app.data.models

data class CrearMiembrosHogarRequest(
    val HOG_ID: Int,
    val cantidadNinos: Int,
    val cantidadAdultos: Int,
    val cantidadAdultosMayores: Int
)