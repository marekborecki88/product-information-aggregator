package com.example.aggregation_service.productdetails.api.exception

class RequiredUpstreamHttpException(
    val upstream: String,
    val upstreamStatus: Int,
    val productId: Int,
    val market: String,
    cause: Throwable? = null
) : RuntimeException(
    "Required upstream '$upstream' returned HTTP $upstreamStatus for product $productId in market $market",
    cause
)