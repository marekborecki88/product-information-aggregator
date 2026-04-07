package com.example.aggregation_service.productdetails.api.dto

import com.example.aggregation_service.productdetails.infrastructure.client.dto.CatalogProductPayload
import com.example.aggregation_service.productdetails.infrastructure.client.dto.PricePayload

data class ProductResponse(
    val id: Int,
    val details: CatalogProductPayload,
    val priceInfo: PricePayload,
    val customerId: Int?
) {

}
