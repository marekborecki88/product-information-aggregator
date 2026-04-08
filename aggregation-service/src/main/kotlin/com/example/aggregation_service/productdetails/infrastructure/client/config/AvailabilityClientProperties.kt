package com.example.aggregation_service.productdetails.infrastructure.client.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "clients.availability")
data class AvailabilityClientProperties(
    val baseUrl: String,
    val connectTimeout: Duration = Duration.ofSeconds(2),
    val readTimeout: Duration = Duration.ofSeconds(3)
)

