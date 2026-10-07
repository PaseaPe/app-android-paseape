package com.example.paseape.domain.model

data class ApplicationParameters(
    val tiposMascota: List<CatalogItem> = emptyList(),
    val distritos: List<CatalogItem> = emptyList(),
    val tiposUsuario: List<CatalogItem> = emptyList()
)
