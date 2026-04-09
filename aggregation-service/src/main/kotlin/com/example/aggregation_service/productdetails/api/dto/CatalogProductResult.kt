package com.example.aggregation_service.productdetails.api.dto

sealed interface CatalogProductResult {
    data class Known(val name: String,
                     val description: String,
                     val specs: Map<String, String>,
                     val images: List<String>
    ) : CatalogProductResult
    data class Unknown(val reason: UnknownReason, val statusCode: Int? = null) : CatalogProductResult
}
