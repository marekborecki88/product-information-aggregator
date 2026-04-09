package com.example.aggregation_service.productdetails.infrastructure.client.dto

data class CatalogProductPayload(
        val name: String,
        val description: String,
        val specs: Map<String, String>,
        val images: List<String>
)

sealed interface CatalogFetchResult {
        data class Found(val product: CatalogProductPayload) : CatalogFetchResult
        data object NotFound : CatalogFetchResult
        data object Timeout : CatalogFetchResult
        data object Unavailable : CatalogFetchResult
        data class HttpError(val status: Int) : CatalogFetchResult
}