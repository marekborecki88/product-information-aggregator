package com.example.aggregation_service.productdetails.infrastructure.client.dto

import com.example.aggregation_service.productdetails.api.dto.AvailabilityResult
import java.time.LocalDate

data class AvailabilityPayload(
    val stockLevel: Int,
    val warehouseLocation: String,
    val expectedDelivery: LocalDate
)

fun AvailabilityPayload.toResult(): AvailabilityResult.Known =
    AvailabilityResult.Known(
        stockLevel = stockLevel,
        warehouseLocation = warehouseLocation,
        expectedDelivery = expectedDelivery
    )

