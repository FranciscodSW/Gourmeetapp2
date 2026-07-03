package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class IngredienteReceta(

    @SerializedName("ING_ID")
    val id: Int,

    @SerializedName("ING_DESCRIPCION")
    val nombre: String,

    @SerializedName("Foto_Ingrediente")
    val foto: String?

)