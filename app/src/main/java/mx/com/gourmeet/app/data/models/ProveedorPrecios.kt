package mx.com.gourmeet.app.data.models

data class PrecioProveedor(val PI_ID: String, val PRECIO: Double, val UNIDAD: String)
data class PreciosProveedorResponse(val success: Boolean, val precios: List<PrecioProveedor>)
data class ReportarProveedorRequest(
    val PRO_ID: Int,
    val PI_ID: Int?,
    val PRECIO_COBRADO: Double?,
    val DESCRIPCION: String,
    val SOLICITUD: String
)
data class ReportarProveedorResponse(val success: Boolean, val message: String)

fun IngredienteProveedor.precioVisible(): String {
    val precio = PRECIO?.takeIf { it.isFinite() && it >= 0 } ?: return "Precio por confirmar"
    val importe = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("es-MX")).apply {
        currency = java.util.Currency.getInstance("MXN")
    }.format(precio)
    return listOfNotNull("$importe MXN", UNIDAD?.takeIf { it.isNotBlank() }).joinToString(" / ")
}

fun List<IngredienteProveedor>.conPrecios(precios: List<PrecioProveedor>): List<IngredienteProveedor> {
    val porId = precios.associateBy { it.PI_ID }
    return map { ingrediente ->
        porId[ingrediente.PI_ID]?.let { ingrediente.copy(PRECIO = it.PRECIO, UNIDAD = it.UNIDAD) } ?: ingrediente
    }
}
