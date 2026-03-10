package com.example.gourmeet2.data.models
import com.google.gson.annotations.SerializedName
data class UsuarioRegistro(

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("correo")
    val correo: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("cli_primer_ip")
    val cliPrimerIp: String,

    @SerializedName("origen")
    val origen: String,

    @SerializedName("edad")
    val edad: Int,

    @SerializedName("nivel")
    val nivel: Int,

    @SerializedName("avatar")
    val avatar: String,

    @SerializedName("latitud")
    val latitud: Double,

    @SerializedName("longitud")
    val longitud: Double,

    @SerializedName("restricciones")
    val restricciones: List<Int>
)
