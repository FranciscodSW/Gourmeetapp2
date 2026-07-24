package com.example.gourmeet2
object NormalizadorTexto {
    private val palabrasIgnorar = setOf(

        "por",
        "favor",
        "puedes",
        "podrias",
        "podrías",
        "quiero",
        "quisiera",
        "me",
        "podrías",
        "podria",
        "ve",
        "ir",
        "al",
        "a",
        "el",
        "la",
        "los",
        "las",
        "un",
        "una",
        "de",
        "del",
        "con",
        "que",
        "porfa"

    )

    /**
     * Convierte el texto en una versión más fácil de analizar.
     */
    fun normalizar(texto: String): String {

        return texto
            .lowercase()
            .replace(",", "")
            .replace(".", "")
            .replace("¿", "")
            .replace("?", "")
            .replace("¡", "")
            .replace("!", "")
            .split(" ")
            .filter {

                it.isNotBlank() &&
                        !palabrasIgnorar.contains(it)

            }
            .joinToString(" ")
            .trim()

    }

}