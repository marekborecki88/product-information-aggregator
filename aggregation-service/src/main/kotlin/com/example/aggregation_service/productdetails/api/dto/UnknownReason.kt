package com.example.aggregation_service.productdetails.api.dto

enum class UnknownReason {
    UPSTREAM_TIMEOUT,
    UPSTREAM_ERROR,
    UPSTREAM_UNAVAILABLE,
    NOT_FOUND;

    fun metricTag(): String = when (this) {
        NOT_FOUND -> "no_price"
        UPSTREAM_TIMEOUT -> "timeout"
        UPSTREAM_UNAVAILABLE -> "unavailable"
        UPSTREAM_ERROR -> "upstream_error"
    }
}