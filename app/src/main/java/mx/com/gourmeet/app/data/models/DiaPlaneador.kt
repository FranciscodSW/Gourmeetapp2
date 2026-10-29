package mx.com.gourmeet.app.data.models

data class DiaPlaneador(
    val fecha: String,
    val nombreDia: String,
    val espacios: List<EspacioComida>,
    var seleccionado: Boolean = true,
    var expandido: Boolean = true,
    var pldId: Int = 0,
    var hogUsuIdCocinero: Int? = null,
    var nombreCocinero: String? = null
)