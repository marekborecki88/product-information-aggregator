package com.example.aggregation_service.productdetails.infrastructure.client.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.example.aggregation_service.productdetails.infrastructure.client.config.HttpClientProperties

@Configuration
class ClientsConfiguration {

    @Bean("http-client.catalog")
    fun catalogClientProperties(clients: ClientsProperties) = clients.catalog


    @Bean("http-client.pricing")
    fun pricingClientProperties(clients: ClientsProperties) = clients.pricing


    @Bean("http-client.availability")
    fun availabilityClientProperties(clients: ClientsProperties) = clients.availability


    @Bean("http-client.customer")
    fun customerClientProperties(clients: ClientsProperties) = clients.customer
}