package mx.com.gourmeet.app.data.models
data class RespuestaComentario(

    val id: Int,

    val idComentario: Int,

    val comentario: String,

    val fecha: String,

    val usuario: UsuarioComentario,

    var likes: Int,

    var dislikes: Int,

    var reportes: Int,

    var miReaccion: String?,

    val reportado: Boolean,

    val esMia: Boolean

)