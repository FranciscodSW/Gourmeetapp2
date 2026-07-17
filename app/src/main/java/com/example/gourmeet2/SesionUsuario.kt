package com.example.gourmeet2.utils

import android.content.Context

object SesionUsuario {

    private const val PREF = "user"

    fun guardarSesion(
        context: Context,
        id: Int,
        nombre: String,
        correo: String,
        foto: String?,
        origen: String?,
        nivel: Int,
        puntos: Int,
        edad: Int
    ){

        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putInt("id", id)
            .putString("nombre", nombre)
            .putString("correo", correo)
            .putString("foto", foto)
            .putString("origen", origen)
            .putInt("nivel", nivel)
            .putInt("edad", edad)
            .putInt("puntos", puntos)
            .apply()

    }

    fun obtenerId(context: Context): Int {

        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getInt("id", 0)

    }

    fun obtenerNombre(context: Context): String {

        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("nombre", "") ?: ""

    }

    fun obtenerCorreo(context: Context): String {

        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("correo", "") ?: ""

    }

    fun obtenerFoto(context: Context): String? {

        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("foto", null)

    }

    fun obtenerOrigen(context: Context): String {

        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("origen", "") ?: ""

    }

    fun obtenerNivel(context: Context): Int {

        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getInt("nivel", 1)

    }

    fun obtenerEdad(context: Context): Int {

        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getInt("edad", 0)

    }

    fun haIniciadoSesion(context: Context): Boolean {

        return obtenerId(context) > 0

    }
    fun actualizarNivel(
        context: Context,
        nivel: Int
    ) {

        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putInt("nivel", nivel)
            .apply()

    }

    fun actualizarPuntos(
        context: Context,
        puntos: Int
    ) {

        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putInt("puntos", puntos)
            .apply()

    }

    fun obtenerPuntos(context: Context): Int {

        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getInt("puntos", 0)

    }

    fun actualizarFoto(
        context: Context,
        foto: String?
    ) {

        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString("foto", foto)
            .apply()

    }

    fun actualizarNombre(
        context: Context,
        nombre: String
    ) {

        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString("nombre", nombre)
            .apply()

    }

    fun actualizarEdad(
        context: Context,
        edad: Int
    ) {

        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putInt("edad", edad)
            .apply()

    }

    fun cerrarSesion(context: Context) {

        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

    }

}