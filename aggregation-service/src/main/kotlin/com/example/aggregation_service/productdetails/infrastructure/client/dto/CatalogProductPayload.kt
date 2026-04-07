package com.example.aggregation_service.productdetails.infrastructure.client.dto

data class CatalogProductPayload(
        val name: String,
        val description: String,
        val specs: Map<String, String>,
        val images: List<String>
)

