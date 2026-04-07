package com.example.aggregation_service.productdetails.infrastructure.client.dto

import com.example.aggregation_service.productdetails.domain.model.Language

data class CatalogProductPayload(
    val name: String,
    val description: String,
    val specs: Map<String, String>,
    val images: List<String>,
    val language: Language
)

