package com.example.aggregation_service

import com.example.aggregation_service.productdetails.infrastructure.client.config.CatalogClientProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(CatalogClientProperties::class)
class AggregationServiceApplication

fun main(args: Array<String>) {
	runApplication<AggregationServiceApplication>(*args)
}
