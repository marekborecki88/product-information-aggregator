package com.example.aggregation_service.productdetails.infrastructure.client.dto

import com.example.aggregation_service.productdetails.domain.valueobject.Money

data class PricePayload(
    val basePrice: Money,
    val customerDiscount: Int,
    val finalPrice: Money
)

