package mx.com.gourmeet.app.data.models

data class AuthResponse(

    val success: Boolean,

    val message: String,

    val usuario: Usuario?
)