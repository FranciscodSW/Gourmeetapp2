package com.example.gourmeet2

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object GestorCaducidad {

    fun calcularFechaCaducidadCarne(
        fechaCompra: String,
        estado: String,
        almacenamiento: String
    ): String? {

        try {

            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            formato.isLenient = false

            val fecha =
                formato.parse(fechaCompra)
                    ?: return null

            val diasEstado = when (estado) {

                "Fresco" -> 9

                "Maduro" -> 9 - 5

                "Pasado" -> 9 - 9

                "Descompuesto" -> 0

                "Sellado" -> return null

                else -> return null
            }

            val calendario =
                Calendar.getInstance()

            calendario.time = fecha

            calendario.add(
                Calendar.DAY_OF_MONTH,
                diasEstado
            )

            val modificadorAlmacenamiento =
                when (almacenamiento) {

                    "Refrigerador" -> 2

                    "Ambiente" -> -1

                    "Congelador" -> 3

                    else -> 0
                }

            calendario.add(
                Calendar.DAY_OF_MONTH,
                modificadorAlmacenamiento
            )

            return formato.format(
                calendario.time
            )

        } catch (e: Exception) {

            Log.e(
                "CADUCIDAD",
                "Error calculando fecha de caducidad de carne",
                e
            )

            return null
        }
    }
    fun calcularFechaCaducidadFrutasVerduras(
        fechaCompra: String,
        estado: String,
        almacenamiento: String
    ): String? {

        try {

            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            formato.isLenient = false

            val fecha =
                formato.parse(fechaCompra)
                    ?: return null

            /*
             * Días que ya tiene el ingrediente
             * dependiendo del estado actual.
             *
             * Verde       = día 0
             * Fresco      = día 3
             * Maduro      = día 6
             * Pasado      = día 9
             * Descompuesto = día 10
             */

            val diasEstado = when (estado) {

                "Verde" -> 10

                "Fresco" -> 10 - 3

                "Maduro" -> 10 - 6

                "Pasado" -> 10 - 9

                "Descompuesto" -> 0

                "Sellado" -> return null

                else -> return null
            }

            val calendario =
                Calendar.getInstance()

            calendario.time = fecha

            calendario.add(
                Calendar.DAY_OF_MONTH,
                diasEstado
            )

            /*
             * Modificador según almacenamiento
             */

            val modificadorAlmacenamiento =
                when (almacenamiento) {

                    "Refrigerador" -> 1

                    "Ambiente" -> -1

                    "Congelador" -> 5

                    else -> 0
                }

            calendario.add(
                Calendar.DAY_OF_MONTH,
                modificadorAlmacenamiento
            )

            return formato.format(
                calendario.time
            )

        } catch (e: Exception) {

            Log.e(
                "CADUCIDAD",
                "Error calculando fecha de caducidad de frutas y verduras",
                e
            )

            return null
        }
    }
    fun calcularFechaCaducidadLacteos(
        fechaCompra: String,
        estado: String,
        almacenamiento: String
    ): String? {

        try {

            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            formato.isLenient = false

            val fecha =
                formato.parse(fechaCompra)
                    ?: return null

            /*
             * Días que ya tiene el ingrediente
             * dependiendo del estado actual.
             *
             * Fresco       = día 0
             * Maduro       = día 3
             * Pasado       = día 4
             * Descompuesto = día 5
             *
             * La función calcula cuántos días faltan
             * para llegar al día 5 (descompuesto).
             */

            val diasEstado = when (estado) {

                "Fresco" -> 5

                "Maduro" -> 5 - 3

                "Pasado" -> 5 - 4

                "Descompuesto" -> 0

                "Sellado" -> return null

                else -> return null
            }

            val calendario =
                Calendar.getInstance()

            calendario.time = fecha

            calendario.add(
                Calendar.DAY_OF_MONTH,
                diasEstado
            )

            /*
             * Modificador según almacenamiento
             */

            val modificadorAlmacenamiento =
                when (almacenamiento) {

                    "Refrigerador" -> 2

                    "Ambiente" -> -1

                    "Congelador" -> 3

                    else -> 0
                }

            calendario.add(
                Calendar.DAY_OF_MONTH,
                modificadorAlmacenamiento
            )

            return formato.format(
                calendario.time
            )

        } catch (e: Exception) {

            Log.e(
                "CADUCIDAD",
                "Error calculando fecha de caducidad de lácteos",
                e
            )

            return null
        }
    }
    fun calcularFechaCaducidadNoPerecedero(
        fechaCompra: String,
        fechaCaducidadActual: String
    ): String? {

        try {

            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            formato.isLenient = false

            // ==============================================
            // FECHA DE CADUCIDAD POR DEFECTO
            // ==============================================

            val fechaCaducidadPorDefecto = "09/10/2026"

            // ==============================================
            // VERIFICAR SI EL USUARIO CAMBIÓ LA FECHA
            // ==============================================

            if (
                fechaCaducidadActual.isNotEmpty() &&
                fechaCaducidadActual != fechaCaducidadPorDefecto
            ) {

                // El usuario cambió la fecha.
                // Se conserva exactamente la fecha introducida.

                return fechaCaducidadActual
            }

            // ==============================================
            // EL USUARIO NO CAMBIÓ LA FECHA
            //
            // Se calcula:
            //
            // FECHA COMPRA + 2 AÑOS
            // ==============================================

            val fecha =
                formato.parse(fechaCompra)
                    ?: return null

            val calendario =
                Calendar.getInstance()

            calendario.time = fecha

            calendario.add(
                Calendar.YEAR,
                2
            )

            // ==============================================
            // DEVOLVER FECHA CALCULADA
            // ==============================================

            return formato.format(
                calendario.time
            )

        } catch (e: Exception) {

            Log.e(
                "CADUCIDAD",
                "Error calculando fecha de caducidad de no perecedero",
                e
            )

            return null
        }
    }
    fun calcularFechaCaducidadAbarrotes(
        fechaCompra: String,
        estado: String
    ): String? {

        try {

            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            formato.isLenient = false

            val fecha =
                formato.parse(fechaCompra)
                    ?: return null

            val calendario =
                Calendar.getInstance()

            calendario.time = fecha

            when (estado) {

                "Sellado" -> {

                    calendario.add(
                        Calendar.YEAR,
                        1
                    )
                }

                "Pasado" -> {

                    calendario.add(
                        Calendar.YEAR,
                        1
                    )
                }

                "Descompuesto" -> {

                    return formato.format(
                        calendario.time
                    )
                }

                else -> {
                    return null
                }
            }

            return formato.format(
                calendario.time
            )

        } catch (e: Exception) {

            Log.e(
                "CADUCIDAD",
                "Error calculando caducidad de abarrotes",
                e
            )

            return null
        }
    }
    fun calcularFechaCaducidadVino(
        fechaCompra: String,
        estado: String,
        almacenamiento: String
    ): String? {

        try {

            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            formato.isLenient = false

            val fecha =
                formato.parse(fechaCompra)
                    ?: return null

            // ==============================================
            // DÍAS HASTA DESCOMPUESTO
            // ==============================================

            val diasEstado = when (estado) {

                "Fresco" -> 13

                "Maduro" -> 13 - 2

                "Pasado" -> 13 - 8

                "Descompuesto" -> 0

                "Sellado" -> return null

                else -> return null
            }

            val calendario =
                Calendar.getInstance()

            calendario.time = fecha

            calendario.add(
                Calendar.DAY_OF_MONTH,
                diasEstado
            )

            // ==============================================
            // ALMACENAMIENTO
            // ==============================================

            val modificadorAlmacenamiento =
                when (almacenamiento) {

                    "Refrigerador" -> 1

                    "Ambiente" -> 0

                    else -> 0
                }

            calendario.add(
                Calendar.DAY_OF_MONTH,
                modificadorAlmacenamiento
            )

            return formato.format(
                calendario.time
            )

        } catch (e: Exception) {

            Log.e(
                "CADUCIDAD",
                "Error calculando caducidad de vino",
                e
            )

            return null
        }
    }
    fun calcularFechaCaducidadEmbutidos(
        fechaCompra: String,
        estado: String,
        almacenamiento: String
    ): String? {

        try {

            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            formato.isLenient = false

            val fecha =
                formato.parse(fechaCompra)
                    ?: return null

            val diasEstado = when (estado) {

                "Verde" -> 7

                "Maduro" -> 7 - 5

                "Pasado" -> 7 - 6

                "Descompuesto" -> 0

                "Sellado" -> return null

                else -> return null
            }

            val calendario =
                Calendar.getInstance()

            calendario.time = fecha

            calendario.add(
                Calendar.DAY_OF_MONTH,
                diasEstado
            )

            val modificadorAlmacenamiento =
                when (almacenamiento) {

                    "Refrigerador" -> 2

                    "Ambiente" -> -1

                    "Congelador" -> 3

                    else -> 0
                }

            calendario.add(
                Calendar.DAY_OF_MONTH,
                modificadorAlmacenamiento
            )

            return formato.format(
                calendario.time
            )

        } catch (e: Exception) {

            Log.e(
                "CADUCIDAD",
                "Error calculando caducidad de embutidos",
                e
            )

            return null
        }
    }
    fun calcularFechaCaducidad(
        fechaCompra: String,
        familia: String?,
        nombreIngrediente: String?,
        estado: String,
        almacenamiento: String,
        fechaCaducidadActual: String = ""
    ): String? {

        if (fechaCompra.isBlank()) {
            return null
        }

        if (estado.isBlank()) {
            return null
        }

        // Sellado utiliza la fecha proporcionada
        // por el usuario. No hacemos cálculo automático.
        if (estado == "Sellado") {
            return null
        }

        val familiaNormalizada =
            familia
                ?.trim()
                ?.lowercase()
                .orEmpty()

        val nombreNormalizado =
            nombreIngrediente
                ?.trim()
                ?.lowercase()
                .orEmpty()

        return when (familiaNormalizada) {

            "cereales leguminosas",
            "especias",
            "pastas",
            "semillas",
            "industrializados",
            "hierbas aromatica" -> {

                calcularFechaCaducidadNoPerecedero(
                    fechaCompra = fechaCompra,
                    fechaCaducidadActual = fechaCaducidadActual
                )
            }

            "abarrotes" -> {

                calcularFechaCaducidadAbarrotes(
                    fechaCompra = fechaCompra,
                    estado = estado
                )
            }

            "licores y destilados" -> {

                if (nombreNormalizado == "vino") {

                    calcularFechaCaducidadVino(
                        fechaCompra = fechaCompra,
                        estado = estado,
                        almacenamiento = almacenamiento
                    )

                } else {
                    null
                }
            }

            "embutidos" -> {

                calcularFechaCaducidadEmbutidos(
                    fechaCompra = fechaCompra,
                    estado = estado,
                    almacenamiento = almacenamiento
                )
            }

            "lacteos" -> {

                calcularFechaCaducidadLacteos(
                    fechaCompra = fechaCompra,
                    estado = estado,
                    almacenamiento = almacenamiento
                )
            }

            "carne" -> {

                calcularFechaCaducidadCarne(
                    fechaCompra = fechaCompra,
                    estado = estado,
                    almacenamiento = almacenamiento
                )
            }

            "fruta",
            "frutas",
            "verdura",
            "verduras" -> {

                calcularFechaCaducidadFrutasVerduras(
                    fechaCompra = fechaCompra,
                    estado = estado,
                    almacenamiento = almacenamiento
                )
            }

            else -> {

                Log.d(
                    "CADUCIDAD",
                    "No existen reglas para la familia: $familia"
                )

                null
            }
        }
    }

}