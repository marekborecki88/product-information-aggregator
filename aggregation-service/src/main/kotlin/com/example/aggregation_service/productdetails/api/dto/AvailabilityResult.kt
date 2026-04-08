package com.example.aggregation_service.productdetails.api.dto

import java.time.LocalDate

sealed class AvailabilityResult {
    data class Known(val stockLevel: Int,
                     val warehouseLocation: String,
                     val expectedDelivery: LocalDate
    ) : AvailabilityResult()
    data class Unknown(
        val reason: AvailabilityUnknownReason
    ) : AvailabilityResult()
}

enum class AvailabilityUnknownReason {
    UPSTREAM_SERVICE_UNAVAILABLE,
    UPSTREAM_SERVICE_TIMEOUT,
    UPSTREAM_SERVICE_ERROR
}
