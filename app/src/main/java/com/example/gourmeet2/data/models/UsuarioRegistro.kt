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
    val cliPrimerIp: String
)
