package mx.com.gourmeet.app.ui

import android.content.Intent
import android.provider.CalendarContract
import mx.com.gourmeet.app.data.models.PasoPreparacion
import mx.com.gourmeet.app.data.models.RecetaDetalle
import java.util.Calendar

object RecipeIntents {
    private fun summary(receta: RecetaDetalle, pasos: List<PasoPreparacion>, localOnly: Boolean): String = buildString {
        appendLine(receta.REC_NOMBRE)
        appendLine()
        appendLine(receta.REC_DESCRIPCION)
        appendLine("Tiempo: ${receta.REC_TIEMPO_PREPARACION} min")
        if (pasos.isNotEmpty()) {
            appendLine()
            appendLine("Preparación")
            pasos.forEachIndexed { index, paso -> appendLine("${index + 1}. ${paso.descripcion}") }
        }
        appendLine()
        if (localOnly) append("Receta del laboratorio de GourMeet.")
        else append("https://www.gourmeet.com.mx/verdetalleReceta.php?REC_ID=${receta.REC_ID}")
    }.take(16000)

    fun share(receta: RecetaDetalle, pasos: List<PasoPreparacion>, localOnly: Boolean): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, receta.REC_NOMBRE)
            putExtra(Intent.EXTRA_TEXT, summary(receta, pasos, localOnly))
        }

    fun calendar(receta: RecetaDetalle, pasos: List<PasoPreparacion>, localOnly: Boolean): Intent {
        val start = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val minutes = receta.REC_TIEMPO_PREPARACION.toIntOrNull()?.coerceIn(5, 720) ?: 60
        return Intent(Intent.ACTION_INSERT).setDataAndType(CalendarContract.Events.CONTENT_URI, "vnd.android.cursor.dir/event").apply {
            putExtra(CalendarContract.Events.TITLE, "Cocinar: ${receta.REC_NOMBRE}")
            putExtra(CalendarContract.Events.DESCRIPTION, summary(receta, pasos, localOnly))
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + minutes * 60_000L)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false)
        }
    }
}
