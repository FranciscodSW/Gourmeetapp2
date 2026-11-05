package mx.com.gourmeet.app.data.models
import com.google.gson.annotations.SerializedName

data class ListaCompraRequest(
    @SerializedName("PLA_ID")
    val plaId: Int
)