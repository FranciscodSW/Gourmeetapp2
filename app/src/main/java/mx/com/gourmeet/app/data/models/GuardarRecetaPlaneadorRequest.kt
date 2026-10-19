package mx.com.gourmeet.app.data.models


data class GuardarRecetaPlaneadorRequest(
    val HOG_ID: Int,
    val CLI_ID: Int,
    val REC_ID: Int,
    val TPC_ID: Int,
    val fechas: List<String>,
    val ECR_PORCIONES: Double,
    val ECM_NUMERO_COLACION: Int = 0
)

data class GuardarRecetaPlaneadorResponse(
    val success: Boolean,
    val message: String,
    val PLA_ID: Int? = null,
    val dias_guardados: Int? = null,
    val espacios: List<EspacioRecetaGuardada>? = null
)

data class EspacioRecetaGuardada(
    val ECM_ID: Int,
    val REC_ID: Int,
    val ECM_FECHA: String,
    val TPC_ID: Int,
    val ECM_NUMERO_COLACION: Int,
    val ECR_PORCIONES: Double
)

data class ActualizarHoraEspacioRequest(
    val HOG_ID: Int,
    val CLI_ID: Int,
    val ECM_ID: Int,
    val ECM_HORA: String?
)

data class ActualizarHoraEspacioResponse(
    val success: Boolean,
    val message: String
)
