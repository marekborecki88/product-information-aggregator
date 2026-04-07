package com.example.aggregation_service.productdetails.infrastructure.client.dto

import java.time.LocalDate

data class ProductAvailabilityPayload(
    val stockLevel: Int,
    val warehouseLocation: String,
    val expectedDelivery: LocalDate
) {
}