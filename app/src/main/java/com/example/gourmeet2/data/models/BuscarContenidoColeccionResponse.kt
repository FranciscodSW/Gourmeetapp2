package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class BuscarContenidoColeccionResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("ingredientes")
    val ingredientes: List<IngredienteBusqueda> = emptyList(),

    @SerializedName("recetas")
    val recetas: List<RecetaconFiltro> = emptyList(),

    @SerializedName("categorias")
    val categorias: List<CategoriaBusqueda> = emptyList()

)