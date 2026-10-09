package mx.com.gourmeet.app.data.models

data class ResultadoBusqueda(

    val tipo: Int,

    val id: Int = 0,

    val titulo: String = "",

    val nombre: String = "",

    val foto: String? = null,

    val color: String? = null,

    val recetas: List<RecetaconFiltro> = emptyList()

) {

    companion object {

        const val TITULO = 0

        const val INGREDIENTE = 1

        const val RECETA = 2

        const val CATEGORIA = 3

        const val RECETAS_HORIZONTAL = 4
    }
}