package com.example.aggregation_service.productdetails.api.exception

class RequiredUpstreamUnavailableException(
    val upstream: String,
    val productId: Int,
    val market: String,
    cause: Throwable? = null
) : RuntimeException(
    "Required upstream '$upstream' is unavailable for product $productId in market $market",
    cause
)