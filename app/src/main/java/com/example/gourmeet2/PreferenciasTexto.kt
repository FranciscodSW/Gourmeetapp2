package com.example.gourmeet2

import android.content.Context

object PreferenciasTexto {

    private const val NOMBRE_PREFERENCIAS =
        "PreferenciasGourMeet"

    private const val CLAVE_TAMANO_LETRA =
        "tamano_letra"


    // ==========================================
    // TAMAÑOS DISPONIBLES
    // ==========================================

    const val PEQUENA = 0
    const val NORMAL = 1
    const val GRANDE = 2
    const val MUY_GRANDE = 3


    // ==========================================
    // GUARDAR TAMAÑO
    // ==========================================

    fun guardarTamano(
        context: Context,
        tamano: Int
    ) {

        context
            .getSharedPreferences(
                NOMBRE_PREFERENCIAS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putInt(
                CLAVE_TAMANO_LETRA,
                tamano
            )
            .apply()
    }


    // ==========================================
    // OBTENER TAMAÑO
    // ==========================================

    fun obtenerTamano(
        context: Context
    ): Int {

        return context
            .getSharedPreferences(
                NOMBRE_PREFERENCIAS,
                Context.MODE_PRIVATE
            )
            .getInt(
                CLAVE_TAMANO_LETRA,
                NORMAL
            )
    }


    // ==========================================
    // OBTENER ESCALA
    // ==========================================

    fun obtenerEscala(
        context: Context
    ): Float {

        return when (
            obtenerTamano(context)
        ) {

            PEQUENA ->
                0.85f

            NORMAL ->
                1.00f

            GRANDE ->
                1.15f

            MUY_GRANDE ->
                1.30f

            else ->
                1.00f
        }
    }
}