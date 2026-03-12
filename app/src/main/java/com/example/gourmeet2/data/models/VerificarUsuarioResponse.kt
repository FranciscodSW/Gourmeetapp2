package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class VerificarUsuarioResponse(

    val success: Boolean,

    @SerializedName("correo_existe")
    val correoExiste: Boolean,

    @SerializedName("nombre_existe")
    val nombreExiste: Boolean
)