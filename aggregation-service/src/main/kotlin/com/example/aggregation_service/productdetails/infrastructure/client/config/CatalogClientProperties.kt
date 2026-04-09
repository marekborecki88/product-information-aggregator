package com.example.aggregation_service.productdetails.infrastructure.client.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "clients.catalog")
data class CatalogClientProperties(
    val baseUrl: String,
    val timeout: Duration = Duration.ofSeconds(400)
)

