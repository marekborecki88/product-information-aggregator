package com.example.aggregation_service.productdetails.api.exception

class RequiredUpstreamTimeoutException(
    val upstream: String,
    val productId: Int,
    val market: String,
    cause: Throwable? = null
) : RuntimeException(
    "Required upstream '$upstream' timed out for product $productId in market $market",
    cause
)