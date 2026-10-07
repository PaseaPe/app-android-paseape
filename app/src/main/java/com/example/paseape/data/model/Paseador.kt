package com.example.paseape.data.model

data class Paseador(
    val id: Int,
    val uuid: String,
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val telefono: String?,
    val foto_perfil_url: String?,
    val experiencia_anos: Int,
    val biografia: String?,
    val tarifa_hora_pen: Double,
    val distrito_cobertura_id: Int,
    val distrito_nombre: String,
    val paseador_estado_verificacion_id: Int, // 3 = Verificado, etc.
    val calificacion_promedio: Double,
    val paseos_completados: Int
)