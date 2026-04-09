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
        val reason: UnknownReason
    ) : AvailabilityResult() {
        override val status = "unknown"
    }
}
