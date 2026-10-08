package com.example.gourmeet2

import com.example.gourmeet2.data.models.MiembroHogar
import com.example.gourmeet2.data.models.UsuarioBusqueda

sealed class MiembroEditarItem {

    data class Existente(
        val miembro: MiembroHogar
    ) : MiembroEditarItem()

    data class Nuevo(
        val usuario: UsuarioBusqueda
    ) : MiembroEditarItem()
}