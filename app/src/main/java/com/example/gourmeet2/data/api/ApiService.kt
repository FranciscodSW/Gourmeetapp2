package com.example.gourmeet2.data.api

import  com.example.gourmeet2.data.models.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
interface ApiService {
    @GET("usuario/obtener_restricciones.php")
    suspend fun obtenerRestricciones(): RestriccionesResponse
    @POST("usuario/RegistroUsuario.php")
    suspend fun registrarUsuario(
        @Body request: UsuarioRegistro
    ): AuthResponse
    @POST("usuario/login_usuario.php")
    suspend fun loginUsuario(
        @Body request: Login
    ): AuthResponse
    @POST("usuario/verificar_usuario.php")
    suspend fun verificarUsuario(
        @Body request: VerificarUsuario
    ): VerificarUsuarioResponse
    @POST("usuario/RegistroUsuarioGoogle.php")
    suspend fun registroGoogle(
        @Body request: RegistroGoogle
    ): AuthResponse
    @POST("usuario/login_google.php")
    suspend fun loginUsuarioGoogle(
        @Body request: LoginGoogle
    ): AuthResponse
    @POST("usuario/RegistroUsuarioFacebook.php")
    suspend fun registroFacebook(
        @Body request: FacebookRegistro
    ): AuthResponse
    @POST("usuario/Login_Facebook.php")
    suspend fun loginFacebook(
        @Body request: LoginFacebook
    ): AuthResponse
    @GET("ingredientes/api_autocomplete_ingredientes.php")
    suspend fun autocompleteIngredientes(
        @Query("busqueda") busqueda: String
    ): BuscarIngredientesResponse
    @GET("recetas/api_autocomplete_recetas.php")
    suspend fun autocompleteRecetas(
        @Query("busqueda") busqueda: String
    ): BuscarRecetasResponse
    @POST("ingredientes/api_filtros_recetas.php")
    suspend fun getFiltrosRecetas(
        @Body request: FiltrosRecetasRequest
    ): FiltrosRecetasResponse
    @POST("recetas/api_filtros_recetas_nombre.php")
    suspend fun getFiltrosRecetasNombre(
        @Body request: FiltrosRecetasNombreRequest
    ): FiltrosRecetasResponse
    @POST("recetas/api_detalle_receta.php")
    suspend fun getDetalleReceta(
        @Body request: DetalleRecetaRequest
    ): DetalleRecetaResponse
    @POST("recetas/api_recetas_inicio.php")
    suspend fun getRecetasInicio(
        @Body request: RecetasInicioRequest
    ): FiltrosRecetasResponse
}