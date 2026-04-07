package com.example.aggregation_service.productdetails.infrastructure.client.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "clients.availability")
data class AvailabilityClientProperties(
    val baseUrl: String
)

