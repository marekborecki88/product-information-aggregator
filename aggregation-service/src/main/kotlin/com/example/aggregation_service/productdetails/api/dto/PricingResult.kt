package com.example.aggregation_service.productdetails.api.dto

import com.example.aggregation_service.productdetails.domain.valueobject.Money

sealed class PricingResult {
    abstract val status: String

    data class Available(
        val basePrice: Money,
        val customerDiscount: Int,
        val finalPrice: Money
    ) : PricingResult() {
        override val status = "available"
    }

    data class Unavailable(
        val reason: PricingUnknownReason,
    ) : PricingResult() {
        override val status = "unavailable"
    }
}

enum class PricingUnknownReason {
    UPSTREAM_TIMEOUT,
    UPSTREAM_ERROR,
    UPSTREAM_UNAVAILABLE,
    NO_PRICE_FOR_MARKET
}

