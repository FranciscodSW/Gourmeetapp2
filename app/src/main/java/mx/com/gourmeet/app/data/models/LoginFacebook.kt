package mx.com.gourmeet.app.data.models

data class LoginFacebook(
    val facebook_id: String,
    val correo: String? = null // opcional
)