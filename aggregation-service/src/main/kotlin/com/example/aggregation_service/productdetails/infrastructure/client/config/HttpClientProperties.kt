package com.example.aggregation_service.productdetails.infrastructure.client.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "http-clients")
data class ClientsProperties(
    val catalog: HttpClientProperties,
    val availability: HttpClientProperties,
    val pricing: HttpClientProperties,
    val customer: HttpClientProperties
)

data class HttpClientProperties(
    val baseUrl: String,
    val connectTimeout: Duration = Duration.ofMillis(100),
    val connectionRequestTimeout: Duration = Duration.ofMillis(100),
    val readTimeout: Duration = Duration.ofMillis(100),
    val timeout: Duration = Duration.ofMillis(200)
)

