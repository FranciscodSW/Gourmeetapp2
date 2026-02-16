package com.example.gourmeet2.data.models
import com.google.gson.annotations.SerializedName
data class BuscarIngredientes (
    @SerializedName("ING_ID") val id: Int,
    @SerializedName("ING_DESCRIPCION") val descripcion: String,
    @SerializedName("Foto_Ingrediente") val foto: String? = null

){
    override fun toString(): String = descripcion
}