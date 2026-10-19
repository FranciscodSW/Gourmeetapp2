
package mx.com.gourmeet.app.data.models

data class EspacioComida(
    val ECM_ID: Int,
    val PLA_ID: Int,
    val TPC_ID: Int,
    val ECM_FECHA: String,
    val ECM_NUMERO_COLACION: Int,
    val ECM_NOTAS: String?,
    val ECM_ESTADO: String,
    var ECM_HORA: String? = null,
    var recetas: List<RecetaEspacio> = emptyList(),
    val PLD_COCINAR: Int = 1
)

data class RecetaEspacio(
    val ECR_ID: Int,
    val REC_ID: Int,
    val REC_NOMBRE: String,
    val ECR_PORCIONES: Double
)

