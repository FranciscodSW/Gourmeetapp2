package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class CategoriaBusqueda(

    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("nombre")
    val nombre: String = "",

    @SerializedName("color")
    val color: String? = null,

    @SerializedName("recetas")
    val recetas: List<RecetaconFiltro>? = emptyList()

)