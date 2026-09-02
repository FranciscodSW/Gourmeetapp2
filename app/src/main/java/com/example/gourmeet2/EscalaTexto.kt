package com.example.gourmeet2

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

object EscalaTexto {


    // ==========================================
    // APLICAR A TODA LA VISTA
    // ==========================================

    fun aplicar(
        root: View,
        context: Context
    ) {

        val escala =
            PreferenciasTexto.obtenerEscala(
                context
            )

        recorrerVista(
            root,
            escala
        )
    }


    // ==========================================
    // RECORRER VISTAS
    // ==========================================

    private fun recorrerVista(
        view: View,
        escala: Float
    ) {

        // ======================================
        // TEXTVIEW
        // ======================================

        if (view is TextView) {

            // Obtener tamaño original
            val tag =
                view.getTag(
                    R.id.tag_tamano_texto_original
                )

            val tamanoOriginal: Float

            if (tag == null) {

                tamanoOriginal =
                    view.textSize /
                            view.resources.displayMetrics.scaledDensity

                view.setTag(
                    R.id.tag_tamano_texto_original,
                    tamanoOriginal
                )

            } else {

                tamanoOriginal =
                    tag as Float
            }


            // ==================================
            // APLICAR ESCALA
            // ==================================

            view.textSize =
                tamanoOriginal * escala
        }


        // ======================================
        // SI TIENE HIJOS
        // ======================================

        if (view is ViewGroup) {

            for (
            i in 0 until view.childCount
            ) {

                recorrerVista(
                    view.getChildAt(i),
                    escala
                )
            }
        }
    }
}