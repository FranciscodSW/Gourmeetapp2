package com.example.gourmeet2.voz

import com.example.gourmeet2.ComparadorTexto

object FiltroEcoTexto {

    /**
     * Indica si el texto reconocido corresponde
     * al mismo texto que está leyendo el TTS.
     */
    fun esEco(
        textoReconocido: String,
        textoLeido: String,
        umbral: Double = 0.90
    ): Boolean {

        if (textoReconocido.isBlank() || textoLeido.isBlank()) {
            return false
        }

        val similitud = ComparadorTexto.calcularSimilitud(
            textoReconocido,
            textoLeido
        )

        return similitud >= umbral
    }

}