package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class RecetaDetalle(

    @SerializedName("REC_ID")
    val REC_ID: Int,

    @SerializedName("REC_NOMBRE")
    val REC_NOMBRE: String,

    @SerializedName("REC_DESCRIPCION")
    val REC_DESCRIPCION: String,

    @SerializedName("REC_DATOGOUMEET")
    val REC_DATOGOUMEET: String,

    @SerializedName("REC_TIEMPO_PREPARACION")
    val REC_TIEMPO_PREPARACION: String,

    @SerializedName("REC_PORCIONES")
    val REC_PORCIONES: Int,

    @SerializedName("Dificultad")
    val Dificultad: String,

    @SerializedName("REC_ENLACEYOUTUBE")
    val REC_ENLACEYOUTUBE: String?,

    @SerializedName("REC_ORIENTACION")
    val REC_ORIENTACION: String?,

    @SerializedName("Categoria")
    val Categoria: String,

    @SerializedName("RC_COLOR")
    val RC_COLOR: String,

    @SerializedName("RC_COMBINARCATEGORIA")
    val RC_COMBINARCATEGORIA: String?,

    @SerializedName("FotoReceta")
    val FotoReceta: String?

)