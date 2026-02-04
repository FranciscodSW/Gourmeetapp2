package com.example.gourmeet2.data.api

import com.example.gourmeet2.data.models.CategoriaResponse
import com.example.gourmeet2.data.models.IngredienteRecetaResponse
import com.example.gourmeet2.data.models.RecetaBuscarResponse
import com.example.gourmeet2.data.models.RecetaResponse
import com.example.gourmeet2.data.models.RecetaRecrcidResponse
import com.example.gourmeet2.data.models.PasosResponse
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

    @GET("buscar_recetas_nombre.php") // Ajusta el endpoint según tu API
    suspend fun buscarRecetas(
        @Query("REC_NOMBRE") query: String,
        @Query("REC_RC_ID") categoriaId: Int
    ): RecetaBuscarResponse
    // NUEVO ENDPOINT
    @GET("listar_ingredientes_recetas.php")
    suspend fun getRecetaConIngredientes(
        @Query("REC_ID") recId: Int
    ): IngredienteRecetaResponse

    @GET("listar_pasos_recetas.php")
    suspend fun getPasosPreparacion(
        @Query("REC_ID") recetaId: Int
    ): PasosResponse

}