package com.example.gourmeet2.data.models


import com.google.gson.annotations.SerializedName

data class UsuarioRegistroResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("error")
    val error: String?
)