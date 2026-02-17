package com.example.gourmeet2.data.api

import  com.example.gourmeet2.data.models.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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
    @GET("buscar_ingredientes.php")
    suspend fun getBuscarIngredientes(
        @Query("q") termino: String
    ): BuscarIngredientesResponse

    @GET("buscar_recetas_ingrediente.php")
    suspend fun getRecetasPorIngredientes(
        @Query("ingredientes") termino: String,
        @Query("categoria") categoria: Int
    ): RecetasPorIngredientesResponse

    @GET("buscar_imagenes_recetas.php")
    suspend fun getImagenesIngredientes(
        @Query("RI_REC_ID") recetaId: Int
    ): ImagenesIngredientesResponse

    @POST("RegistroUsuario.php")
    suspend fun registrarUsuario(
        @Body usuario: UsuarioRegistro
    ): UsuarioRegistroResponse





}