package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class Restriccion(

    @SerializedName("Id_Restricciones")
    val id: Int,

    @SerializedName("Res_Nombre")
    val nombre: String,

    @SerializedName("Res_Descripcion")
    val descripcion: String
)