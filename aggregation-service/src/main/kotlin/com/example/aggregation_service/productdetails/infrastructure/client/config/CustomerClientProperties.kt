package com.example.aggregation_service.productdetails.infrastructure.client.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "clients.customer")
data class CustomerClientProperties(
    val baseUrl: String,
    val connectTimeout: Duration = Duration.ofMillis(200),
    val connectionRequestTimeout: Duration = Duration.ofMillis(50),
    val readTimeout: Duration = Duration.ofMillis(80)
)

