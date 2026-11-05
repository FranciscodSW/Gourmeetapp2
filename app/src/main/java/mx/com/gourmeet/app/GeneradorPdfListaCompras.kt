
package mx.com.gourmeet.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import mx.com.gourmeet.app.data.models.IngredienteListaCompra
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeneradorPdfListaCompras(private val context: Context) {

    private val ancho = 595
    private val alto = 842
    private val margen = 40f
    private val azul = android.graphics.Color.rgb(23, 122, 255)
    private val negro = android.graphics.Color.rgb(35, 35, 35)
    private val gris = android.graphics.Color.rgb(110, 110, 110)
    private val blanco = android.graphics.Color.WHITE

    private val fuente: Typeface =
        ResourcesCompat.getFont(context, R.font.caviardreams)
            ?: Typeface.create("sans-serif", Typeface.NORMAL)

    private val fuenteNegrita = Typeface.create(fuente, Typeface.BOLD)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun generar(
        ingredientes: List<IngredienteListaCompra>,
        plaId: Int
    ): ByteArray {

        val documento = PdfDocument()
        val salida = ByteArrayOutputStream()

        val categorias = ingredientes
            .groupBy {
                it.categoria?.trim()?.ifEmpty { "Sin categoría" }
                    ?: "Sin categoría"
            }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)

        val comprados = ingredientes.count { it.comprado == 1 }
        val pendientes = ingredientes.size - comprados

        var numeroPagina = 1
        var pagina = crearPagina(documento, numeroPagina)
        var canvas = pagina.canvas
        var y = dibujarEncabezado(canvas, plaId, ingredientes.size, comprados, pendientes)

        categorias.forEach { (nombreCategoria, lista) ->

            // Espacio necesario para el título de categoría.
            if (y + 45f > alto - 65f) {
                dibujarPie(canvas, numeroPagina)
                documento.finishPage(pagina)

                numeroPagina++
                pagina = crearPagina(documento, numeroPagina)
                canvas = pagina.canvas
                y = dibujarEncabezado(canvas, plaId, null, null, null)
            }

            y = dibujarCategoria(canvas, nombreCategoria, y)

            lista.forEach { ingrediente ->

                // Cada ingrediente ocupa aproximadamente 48 puntos.
                if (y + 52f > alto - 65f) {
                    dibujarPie(canvas, numeroPagina)
                    documento.finishPage(pagina)

                    numeroPagina++
                    pagina = crearPagina(documento, numeroPagina)
                    canvas = pagina.canvas
                    y = dibujarEncabezado(canvas, plaId, null, null, null)

                    y = dibujarCategoria(canvas, nombreCategoria, y)
                }

                y = dibujarIngrediente(canvas, ingrediente, y)
            }

            y += 12f
        }

        dibujarPie(canvas, numeroPagina)
        documento.finishPage(pagina)

        documento.writeTo(salida)
        documento.close()

        return salida.toByteArray()
    }

    private fun crearPagina(
        documento: PdfDocument,
        numero: Int
    ): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(
            ancho,
            alto,
            numero
        ).create()

        return documento.startPage(info)
    }

    private fun dibujarEncabezado(
        canvas: Canvas,
        plaId: Int,
        total: Int?,
        comprados: Int?,
        pendientes: Int?
    ): Float {

        paint.color = azul
        canvas.drawRoundRect(
            RectF(margen, 30f, ancho - margen, 145f),
            12f,
            12f,
            paint
        )

        configurarPaint(18f, blanco, fuenteNegrita)
        canvas.drawText("GourMeet", margen + 20f, 62f, paint)

        configurarPaint(25f, blanco, fuenteNegrita)
        canvas.drawText("LISTA DE COMPRAS", margen + 20f, 98f, paint)

        configurarPaint(10f, blanco, fuente)
        canvas.drawText(
            "Organiza tus compras de manera sencilla",
            margen + 20f,
            122f,
            paint
        )

        val fecha = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ).format(Date())

        configurarPaint(9f, gris, fuente)


        val fechaTexto = "Generado: $fecha"
        canvas.drawText(
            fechaTexto,
            ancho - margen - paint.measureText(fechaTexto),
            165f,
            paint
        )

        if (total != null && comprados != null && pendientes != null) {
            dibujarResumen(canvas, total, comprados, pendientes)
            return 235f
        }

        return 195f
    }

    private fun dibujarResumen(
        canvas: Canvas,
        total: Int,
        comprados: Int,
        pendientes: Int
    ) {
        val espacio = 8f
        val anchoTarjeta = (ancho - margen * 2 - espacio * 2) / 3f
        val top = 180f
        val bottom = 225f

        val datos = listOf(
            Triple("INGREDIENTES", total, negro),
            Triple("COMPRADOS", comprados, azul),
            Triple("PENDIENTES", pendientes, negro)
        )

        datos.forEachIndexed { index, dato ->
            val left = margen + index * (anchoTarjeta + espacio)

            paint.color = android.graphics.Color.rgb(243, 246, 250)
            canvas.drawRoundRect(
                RectF(left, top, left + anchoTarjeta, bottom),
                7f,
                7f,
                paint
            )

            configurarPaint(15f, dato.third, fuenteNegrita)
            canvas.drawText(
                dato.second.toString(),
                left + 12f,
                top + 21f,
                paint
            )

            configurarPaint(7f, gris, fuente)
            canvas.drawText(
                dato.first,
                left + 12f,
                top + 35f,
                paint
            )
        }
    }

    private fun dibujarCategoria(
        canvas: Canvas,
        nombre: String,
        y: Float
    ): Float {

        paint.color = azul
        canvas.drawRoundRect(
            RectF(margen, y, ancho - margen, y + 27f),
            5f,
            5f,
            paint
        )

        configurarPaint(11f, blanco, fuenteNegrita)
        canvas.drawText(
            nombre.uppercase(Locale.getDefault()),
            margen + 12f,
            y + 18f,
            paint
        )

        return y + 39f
    }

    private fun dibujarIngrediente(
        canvas: Canvas,
        ingrediente: IngredienteListaCompra,
        y: Float
    ): Float {

        val comprado = ingrediente.comprado == 1
        val centroX = margen + 16f
        val centroY = y + 14f
        val radio = 11f

        paint.color = if (comprado) azul else blanco
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centroX, centroY, radio, paint)

        if (!comprado) {
            paint.color = azul
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawCircle(centroX, centroY, radio, paint)
            paint.style = Paint.Style.FILL
        } else {
            configurarPaint(12f, blanco, fuenteNegrita)
            canvas.drawText("✓", centroX - 4f, centroY + 4f, paint)
        }

        val colorNombre = if (comprado) gris else negro

        configurarPaint(10f, colorNombre, fuenteNegrita)
        canvas.drawText(
            ingrediente.descripcion,
            margen + 38f,
            y + 12f,
            paint
        )

        configurarPaint(8f, gris, fuente)

        val cantidad = obtenerCantidad(ingrediente)
        canvas.drawText(
            cantidad,
            margen + 38f,
            y + 27f,
            paint
        )

        if (comprado) {
            paint.color = gris
            paint.strokeWidth = 0.7f
            val anchoNombre = paint.apply {
                textSize = 10f
                typeface = fuenteNegrita
            }.measureText(ingrediente.descripcion)

            canvas.drawLine(
                margen + 38f,
                y + 9f,
                margen + 38f + anchoNombre,
                y + 9f,
                paint
            )
        }

        paint.color = android.graphics.Color.rgb(230, 230, 230)
        paint.strokeWidth = 0.5f
        canvas.drawLine(
            margen + 38f,
            y + 37f,
            ancho - margen,
            y + 37f,
            paint
        )

        return y + 48f
    }

    private fun obtenerCantidad(
        ingrediente: IngredienteListaCompra
    ): String {

        val unidad = ingrediente.unidad?.trim().orEmpty()

        if (unidad.equals("Al gusto", ignoreCase = true)) {
            return "Al gusto"
        }

        if (unidad.equals("Por revisar", ignoreCase = true)) {
            return "Por revisar"
        }

        val cantidad = ingrediente.cantidad
            ?: return unidad.ifEmpty { "Por revisar" }

        val cantidadTexto = String.format(
            Locale.getDefault(),
            "%.3f",
            cantidad
        ).trimEnd('0').trimEnd(',', '.')

        return listOf(cantidadTexto, unidad)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    private fun dibujarPie(canvas: Canvas, numeroPagina: Int) {
        paint.color = android.graphics.Color.rgb(220, 220, 220)
        paint.strokeWidth = 0.6f
        canvas.drawLine(
            margen,
            alto - 48f,
            ancho - margen,
            alto - 48f,
            paint
        )

        configurarPaint(8f, gris, fuente)
        canvas.drawText(
            "GourMeet · Tu cocina organizada",
            margen,
            alto - 30f,
            paint
        )

        val textoPagina = "Página $numeroPagina"
        canvas.drawText(
            textoPagina,
            ancho - margen - paint.measureText(textoPagina),
            alto - 30f,
            paint
        )
    }

    private fun configurarPaint(
        tamano: Float,
        color: Int,
        tipo: Typeface
    ) {
        paint.apply {
            this.textSize = tamano
            this.color = color
            this.typeface = tipo
            this.style = Paint.Style.FILL
            this.strokeWidth = 1f
        }
    }
}
