package com.example.aggregation_service.productdetails.api.dto

import java.time.LocalDate

sealed class AvailabilityResult {
    abstract val status: String

    data class Known(val stockLevel: Int,
                     val warehouseLocation: String,
                     val expectedDelivery: LocalDate
    ) : AvailabilityResult() {
        override val status = "known"
    }

    data class Unknown(
        val reason: AvailabilityUnknownReason
    ) : AvailabilityResult() {
        override val status = "unknown"
    }
}

enum class AvailabilityUnknownReason {
    UPSTREAM_SERVICE_UNAVAILABLE,
    UPSTREAM_SERVICE_TIMEOUT,
    UPSTREAM_SERVICE_ERROR
}
