package com.example.aggregation_service.productdetails.api.dto

import com.example.aggregation_service.productdetails.infrastructure.client.dto.CatalogProductPayload
import com.example.aggregation_service.productdetails.infrastructure.client.dto.CustomerPayload
import com.example.aggregation_service.productdetails.infrastructure.client.dto.ResolvedCustomerContext

data class ProductResponse(
    val id: Int,
    val details: CatalogProductPayload,
    val pricing: PricingResult,
    val availability: AvailabilityResult,
    val personalization: ResolvedCustomerContext,
)
