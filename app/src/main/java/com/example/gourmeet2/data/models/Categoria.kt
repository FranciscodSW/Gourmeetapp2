package com.example.gourmeet2.data.models

import com.google.gson.annotations.SerializedName

data class Categoria(
    @SerializedName("RC_ID") val id: Int,
    @SerializedName("RC_DESCRIPCION") val descripcion: String,
    @SerializedName("RC_COLOR") val color: String

)