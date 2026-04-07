package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.application.port.out.CustomerClient
import com.example.aggregation_service.productdetails.infrastructure.client.config.CustomerClientProperties
import com.example.aggregation_service.productdetails.infrastructure.client.dto.CustomerPayload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class CustomerClientHttp(
    properties: CustomerClientProperties
) : CustomerClient {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .build()

    override fun findByCustomerId(customerId: Int): CustomerPayload? {
        return try {
            restClient.get()
                .uri("/customer-context/{customerId}", customerId)
                .retrieve()
                .body(CustomerPayload::class.java)
        } catch (ex: RestClientResponseException) {
            log.warn("Failed to fetch customer context [customerId=$customerId]: ${ex.statusCode}")
            null
        }
    }
}

