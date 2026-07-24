package com.example.gourmeet2

class AdministradorComandosVoz {

    /**
     * Similitud mínima para aceptar un comando.
     * Puedes ajustarla entre 0.70 y 0.90.
     */
    private val umbralSimilitud = 0.75

    /**
     * Catálogo de comandos y sus sinónimos.
     */
    private val comandos = mapOf(

        ComandoVoz.SIGUIENTE to listOf(
            "siguiente",
            "siguiente paso",
            "continua",
            "continúa",
            "continuar",
            "avanza",
            "sigue",
            "próximo",
            "proximo"
        ),

        ComandoVoz.ANTERIOR to listOf(
            "anterior",
            "paso anterior",
            "regresa",
            "retrocede",
            "atrás",
            "atras"
        ),

        ComandoVoz.REPETIR to listOf(
            "repite",
            "repetir",
            "otra vez",
            "léelo otra vez",
            "leelo otra vez"
        ),

        ComandoVoz.DETENER to listOf(
            "detener",
            "detente",
            "para",
            "alto",
            "silencio"
        ),

        ComandoVoz.CONTINUAR to listOf(
            "continuar",
            "continúa",
            "continua"
        ),

        ComandoVoz.VER_TODO to listOf(
            "ver todo",
            "mostrar todo",
            "mostrar pasos",
            "ver pasos"
        ),

        ComandoVoz.OCULTAR to listOf(
            "cerrar",
            "ocultar",
            "ver menos",
            "cerrar pasos"
        )

    )

    /**
     * Analiza el texto reconocido y devuelve el comando
     * que más se parece.
     */
    fun analizarTexto(texto: String): ComandoVoz {

        val textoNormalizado =
            NormalizadorTexto.normalizar(texto)

        var mejorComando = ComandoVoz.DESCONOCIDO
        var mejorSimilitud = 0.0

        for ((comando, sinonimos) in comandos) {

            for (sinonimo in sinonimos) {

                val similitud =
                    ComparadorTexto.calcularSimilitud(
                        textoNormalizado,
                        sinonimo
                    )

                if (similitud > mejorSimilitud) {

                    mejorSimilitud = similitud
                    mejorComando = comando

                }

                /**
                 * También buscamos si alguna palabra del texto
                 * es muy parecida al sinónimo.
                 */

                textoNormalizado.split(" ").forEach { palabra ->

                    val similitudPalabra =
                        ComparadorTexto.calcularSimilitud(
                            palabra,
                            sinonimo
                        )

                    if (similitudPalabra > mejorSimilitud) {

                        mejorSimilitud = similitudPalabra
                        mejorComando = comando

                    }

                }

            }

        }

        return if (mejorSimilitud >= umbralSimilitud) {

            mejorComando

        } else {

            ComandoVoz.DESCONOCIDO

        }

    }

}