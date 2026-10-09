package mx.com.gourmeet.app.data.models

data class RestriccionesData(
    val alergia: List<Restriccion>,
    val alimento: List<Restriccion>,
    val cultural: List<Restriccion>,
    val intolerancia: List<Restriccion>
)