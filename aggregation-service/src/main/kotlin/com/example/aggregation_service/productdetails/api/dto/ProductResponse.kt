package com.example.aggregation_service.productdetails.api.dto

data class ProductResponse(
    val id: Int,
    val details: CatalogProductResult,
    val pricing: PricingResult,
    val availability: AvailabilityResult,
    val personalization: ResolvedCustomerContext,
)
