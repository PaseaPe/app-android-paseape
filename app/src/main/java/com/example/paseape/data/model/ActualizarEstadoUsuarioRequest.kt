package com.example.paseape.data.model

import com.google.gson.annotations.SerializedName

data class ActualizarEstadoUsuarioRequest(
    @SerializedName("usuario_estado_id")
    val usuarioEstadoId: Int
)