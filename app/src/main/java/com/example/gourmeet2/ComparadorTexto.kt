package com.example.gourmeet2

import kotlin.math.min

object ComparadorTexto {

    /**
     * Devuelve un porcentaje de similitud entre dos textos.
     * 1.0 = idénticos
     * 0.0 = completamente diferentes
     */
    fun calcularSimilitud(
        texto1: String,
        texto2: String
    ): Double {

        val t1 = texto1.lowercase().trim()
        val t2 = texto2.lowercase().trim()

        if (t1 == t2) return 1.0

        val distancia = calcularDistanciaLevenshtein(t1, t2)

        val longitudMayor = maxOf(t1.length, t2.length)

        if (longitudMayor == 0)
            return 1.0

        return 1.0 - distancia.toDouble() / longitudMayor
    }

    /**
     * Distancia de Levenshtein.
     */
    private fun calcularDistanciaLevenshtein(
        origen: String,
        destino: String
    ): Int {

        val matriz = Array(origen.length + 1) {
            IntArray(destino.length + 1)
        }

        for (i in matriz.indices)
            matriz[i][0] = i

        for (j in 0..destino.length)
            matriz[0][j] = j

        for (i in 1..origen.length) {

            for (j in 1..destino.length) {

                val costo =
                    if (origen[i - 1] == destino[j - 1]) 0 else 1

                matriz[i][j] = min(
                    min(
                        matriz[i - 1][j] + 1,
                        matriz[i][j - 1] + 1
                    ),
                    matriz[i - 1][j - 1] + costo
                )
            }
        }

        return matriz[origen.length][destino.length]
    }

}