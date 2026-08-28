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
    @POST("recetas/comentar_calificar_receta_api.php")
    suspend fun comentarCalificarReceta(
        @Body request: ComentarCalificarRequest
    ): ComentarCalificarResponse
    @POST("recetas/obtener_mi_comentario_api.php")
    suspend fun obtenerMiComentario(
        @Body request: ObtenerMiComentarioRequest
    ): ObtenerMiComentarioResponse
    @POST("recetas/marcar_receta_terminada_api.php")
    suspend fun marcarRecetaTerminada(
        @Body request: MarcarRecetaTerminadaRequest
    ): MarcarRecetaTerminadaResponse
    @POST("recetas/verificar_receta_terminada_api.php")
    suspend fun verificarRecetaTerminada(
        @Body request: VerificarRecetaTerminadaRequest
    ): VerificarRecetaTerminadaResponse
    @POST("comentarios/listar_comentarios.php")
    suspend fun listarComentarios(
        @Body request: ListarComentariosRequest
    ): ListarComentariosResponse
    @POST("comentarios/reaccionar_comentario.php")
    suspend fun reaccionarComentario(
        @Body request: ReaccionComentarioRequest
    ): ReaccionComentarioResponse
    @POST("comentarios/responder_comentario.php")
    suspend fun responderComentario(
        @Body request: RespuestaComentarioRequest
    ): RespuestaComentarioResponse
    @POST("comentarios/eliminar_comentario.php")
    suspend fun eliminarComentario(
        @Body request: EliminarComentarioRequest
    ): EliminarComentarioResponse
    @POST("comentarios/reaccionar_comentario_respuesta.php")
    suspend fun reaccionarRespuesta(
        @Body request: ReaccionRespuestaRequest
    ): ReaccionRespuestaResponse
    @POST("recetas/reportar_receta_api.php")
    suspend fun reportarReceta(
        @Body request: ReportarRecetaRequest
    ): ReportarRecetaResponse
    @POST("colecciones/listar_mis_colecciones.php")
    suspend fun listarMisColecciones(
        @Body request: ListarColeccionesRequest
    ): ListarColeccionesResponse
    @POST("colecciones/listar_colecciones_con_recetas.php")
    suspend fun listarColeccionesConRecetas(
        @Body request: ListarColeccionesRecetasRequest
    ): ListarColeccionesRecetasResponse
    @POST("colecciones/crear_coleccion.php")
    suspend fun crearColeccion(
        @Body request: CrearColeccionRequest
    ): CrearColeccionResponse
    @POST("colecciones/buscar_contenido_coleccion.php")
    suspend fun buscarContenidoColeccion(
        @Body request: BuscarContenidoColeccionRequest
    ): BuscarContenidoColeccionResponse
    @POST("ingredientes/buscar_recetas_por_ingredientes.php")
    suspend fun buscarRecetasPorIngredientes(
        @Body request: BuscarRecetasPorIngredientesRequest
    ): BuscarRecetasPorIngredientesResponse
    @POST("colecciones/eliminar_coleccion.php")
    suspend fun eliminarColeccion(
        @Body request: EliminarColeccionRequest
    ): EliminarColeccionResponse
    @POST("colecciones/actualizar_coleccion.php")
    suspend fun actualizarColeccion(
        @Body request: ActualizarColeccionRequest
    ): ActualizarColeccionResponse
    @POST("colecciones/gestionar_favorito.php")
    suspend fun gestionarFavorito(
        @Body request: GestionarFavoritoRequest
    ): GestionarFavoritoResponse
    @POST("colecciones/verificar_favorito.php")
    suspend fun verificarFavorito(
        @Body request: VerificarFavoritoRequest
    ): VerificarFavoritoResponse
    @POST("colecciones/gestionar_receta_coleccion.php")
    suspend fun gestionarRecetaColeccion(
        @Body request: GestionarRecetaColeccionRequest
    ): GestionarRecetaColeccionResponse
    @POST("recetas_realizadas/listar_recetas_realizadas.php")
    suspend fun listarRecetasRealizadas(
        @Body request: RecetasRealizadasRequest
    ): RecetasRealizadasResponse
    @GET("proveedores/listar_proveedores.php")
    suspend fun listarProveedores(): ProveedoresResponse
    @GET("proveedores/detalle_proveedor.php")
    suspend fun obtenerDetalleProveedor(
        @Query("ID_PROVEEDOR") idProveedor: Int
    ): DetalleProveedorResponse
    @GET("proveedores/listar_recetas_proveedor.php")
    suspend fun listarRecetasProveedor(
        @Query("idProveedor") idProveedor: Int
    ): RecetasProveedorResponse
    @POST("colecciones/agregar_proveedor_coleccion.php")
    suspend fun crearProveedorColeccion(
        @Body datos: CrearColeccionProveedor
    ): CrearColeccionProveedorResponse

    @POST("colecciones/listar_colecciones_proveedores.php")
    suspend fun listarColeccionesProveedores(
        @Body datos: ConsultarColeccionesProveedor
    ): ListarColeccionesProveedoresResponse

}