package com.example.gourmeet2

import android.content.Context

object UtilidadesTamanoLetra {

    fun obtenerEscala(context: Context): Float {

        val preferencias =
            context.getSharedPreferences(
                "PreferenciasGourMeet",
                Context.MODE_PRIVATE
            )

        return when (
            preferencias.getInt(
                "tamano_letra",
                1
            )
        ) {

            0 -> 0.85f      // Pequeña

            1 -> 1.00f      // Normal

            2 -> 1.15f      // Grande

            3 -> 1.30f      // Muy grande

            else -> 1.00f
        }
    }
}