package mx.com.gourmeet.app.data.models

data class ColacionGuardadaData(
    val ECM_ID: Int,
    val PLA_ID: Int,
    val TPC_ID: Int,
    val ECM_FECHA: String,
    val ECM_HORA: String?,
    val modalidad: String
)