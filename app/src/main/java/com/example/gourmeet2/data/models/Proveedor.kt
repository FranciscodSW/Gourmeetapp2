package com.example.gourmeet2.data.models

data class Proveedor(
    val Id_Proveedor: String,
    val Pro_nombre: String?,
    val Pro_Correo: String?,
    val Pro_Telefono: String?,
    val Pro_Descripcion: String?,
    val Pro_Facebook: String?,
    val Pro_Instagram: String?,
    val Pro_Tiktok: String?,
    val Pro_Foto: String?,
    val Pro_Direccion: String?,
    val Pro_Hora_Apertura: String?,
    val Pro_Hora_Cierre: String?,
    val Pro_Dias: String?,
    val Pro_Tipo_Entrega: String?,
    val Pro_Estatus: String?,
    val Pro_Latitud: String?,
    val Pro_Longitud: String?,
    val Pro_GIRO: String?,
    val Pro_Des_Giro: String?,
    val Pro_Foto_Perfil: String?,
    val TIPO: String?,
    val CATEGORIAS: List<String>,
    val CATEGORIAS_RECETAS: List<String>,
    val INGREDIENTES: List<IngredienteProveedor>,
    val RECETAS: List<RecetaProveedor>
)