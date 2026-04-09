package com.example.aggregation_service.productdetails.api.dto

data class CustomerPreference(
    val type: String,
    val value: String
)

sealed interface CustomerResult {
    data class Available(val segment: String,
                         val preferences: List<CustomerPreference> = emptyList()
    ) : CustomerResult
    data class Unavailable(val reason: UnknownReason) : CustomerResult
}

data class ResolvedCustomerContext(
    val segment: String,
    val preferences: List<CustomerPreference>,
    val personalized: Boolean,
    val responseDegraded: Boolean
)

