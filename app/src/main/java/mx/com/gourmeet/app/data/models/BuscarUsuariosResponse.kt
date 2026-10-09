package mx.com.gourmeet.app.data.models

data class BuscarUsuariosResponse(
    val success: Boolean,
    val mensaje: String,
    val usuarios: List<UsuarioBusqueda>
)