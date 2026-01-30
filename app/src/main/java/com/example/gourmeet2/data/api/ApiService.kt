package com.example.gourmeet2.data.api

import com.example.gourmeet2.data.models.CategoriaResponse
import com.example.gourmeet2.data.models.RecetaResponse
import com.example.gourmeet2.data.models.RecetaRecrcidResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("listar_recetas_api.php")
    suspend fun getRecetas(
        @Query("pagina") pagina: Int = 1,
        @Query("limite") limite: Int = 100
    ): RecetaResponse
    @GET("listar_descripcio_color.php")
    suspend fun getCategorias(): CategoriaResponse

    @GET("listar_recetas_rec_rc_id_api.php")
    suspend fun getRecetasPorCategoria(
        @Query("categoria_id") categoriaId: Int // Sin valor por defecto
    ): RecetaRecrcidResponse


}