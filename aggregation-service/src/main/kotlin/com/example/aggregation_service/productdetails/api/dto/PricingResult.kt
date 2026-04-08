package com.example.aggregation_service.productdetails.api.dto

import com.example.aggregation_service.productdetails.domain.valueobject.Money

sealed class PricingResult {
    data class Available(
        val basePrice: Money,
        val customerDiscount: Int,
        val finalPrice: Money
    ) : PricingResult()

    data class Unavailable(
        val reason: PricingUnknownReason,
    ) : PricingResult()
}

enum class PricingUnknownReason {
    UPSTREAM_TIMEOUT,
    UPSTREAM_ERROR,
    UPSTREAM_UNAVAILABLE,
    NO_PRICE_FOR_MARKET
}

