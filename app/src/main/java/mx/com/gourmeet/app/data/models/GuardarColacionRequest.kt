package mx.com.gourmeet.app.data.models

data class GuardarColacionRequest(
    val PLA_ID: Int,
    val ECM_FECHA: String,
    val ECM_HORA: String?,
    val ECM_NUMERO_COLACION: Int,
    val ECM_NOTAS: String? = null,
    val REC_ID: Int? = null,
    val ECR_PORCIONES: Double = 1.0,
    val ingredientes: List<IngredienteColacionRequest> = emptyList()
)