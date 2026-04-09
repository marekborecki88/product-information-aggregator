package com.example.aggregation_service.productdetails.infrastructure.client.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "clients.pricing")
data class PricingClientProperties(
    val baseUrl: String,
    val connectTimeout: Duration = Duration.ofMillis(100),
    val readTimeout: Duration = Duration.ofMillis(100),
    val timeout: Duration = Duration.ofMillis(200)
)

