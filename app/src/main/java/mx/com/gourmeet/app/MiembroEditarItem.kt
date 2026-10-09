package mx.com.gourmeet.app

import mx.com.gourmeet.app.data.models.*


sealed class MiembroEditarItem {

    data class Existente(
        val miembro: MiembroHogar
    ) : MiembroEditarItem()

    data class Nuevo(
        val usuario: UsuarioBusqueda
    ) : MiembroEditarItem()
}