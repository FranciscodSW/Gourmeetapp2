package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class BuscarRecetas(

    @SerializedName("REC_ID")
    val id: Int,

    @SerializedName("REC_NOMBRE")
    val nombre: String,

    @SerializedName("FotoReceta")
    val foto: String? = null

) {
    override fun toString(): String = nombre
}