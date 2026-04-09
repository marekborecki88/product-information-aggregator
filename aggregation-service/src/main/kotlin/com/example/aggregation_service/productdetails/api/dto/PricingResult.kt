package com.example.aggregation_service.productdetails.api.dto

import com.example.aggregation_service.productdetails.domain.valueobject.Money

sealed class PricingResult {
    abstract val status: String

    data class Known(
        val basePrice: Money,
        val customerDiscount: Int,
        val finalPrice: Money
    ) : PricingResult() {
        override val status = "known"
    }

    data class Unknown(
        val reason: UnknownReason,
    ) : PricingResult() {
        override val status = "unknown"
    }
}


