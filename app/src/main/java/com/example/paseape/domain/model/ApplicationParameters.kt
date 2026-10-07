package com.example.paseape.domain.model

data class ApplicationParameters(
    val tiposMascota: List<CatalogItem> = emptyList(),
    val distritos: List<CatalogItem> = emptyList(),
    val tiposUsuario: List<CatalogItem> = emptyList(),
    val tiposDocumento: List<CatalogItem> = emptyList(),
    val generosMascota: List<CatalogItem> = emptyList(),
    val tamanosMascota: List<CatalogItem> = emptyList(),
    val nivelesEnergia: List<CatalogItem> = emptyList()
)
