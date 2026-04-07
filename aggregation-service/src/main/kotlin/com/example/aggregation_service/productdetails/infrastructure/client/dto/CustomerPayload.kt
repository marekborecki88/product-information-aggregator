package com.example.aggregation_service.productdetails.infrastructure.client.dto

data class CustomerPayload(
    val segment: String,
    val preferences: List<CustomerPreference> = emptyList()
)

data class CustomerPreference(
    val type: String,
    val value: String
)

