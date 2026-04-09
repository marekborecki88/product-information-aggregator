package com.example.aggregation_service.productdetails.infrastructure.client.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "clients.customer")
data class CustomerClientProperties(
    val baseUrl: String,
    val timeout: Duration = Duration.ofMillis(200),
)

