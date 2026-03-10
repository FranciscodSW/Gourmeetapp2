package com.example.gourmeet2.data.models


import com.google.gson.annotations.SerializedName

data class RegistroResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("usuario_id")
    val usuarioId: Int?,

    @SerializedName("message")
    val message: String?
)