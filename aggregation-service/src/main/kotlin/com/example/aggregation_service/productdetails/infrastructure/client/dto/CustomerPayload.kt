package com.example.aggregation_service.productdetails.infrastructure.client.dto

data class CustomerPayload(
    val segment: String,
    val preferences: List<CustomerPreference> = emptyList()
)

data class CustomerPreference(
    val type: String,
    val value: String
)

sealed interface CustomerLookupResult {
    data class Found(val payload: CustomerPayload) : CustomerLookupResult
    data object NoCustomerId : CustomerLookupResult
    data object NotFound : CustomerLookupResult
    data object TimedOut : CustomerLookupResult
    data class HttpError(val status: Int) : CustomerLookupResult
}

data class ResolvedCustomerContext(
    val segment: String,
    val preferences: List<CustomerPreference>,
    val personalized: Boolean,
    val responseDegraded: Boolean
)

