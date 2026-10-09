package mx.com.gourmeet.app.data.models

data class ReaccionComentarioResponse(

    val success: Boolean,

    val comentario: Int,

    val miReaccion: String?,

    val likes: Int,

    val dislikes: Int,

    val reportes: Int

)