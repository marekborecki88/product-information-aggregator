package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.application.port.out.AvailabilityClient
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.ProductId
import com.example.aggregation_service.productdetails.infrastructure.client.config.AvailabilityClientProperties
import com.example.aggregation_service.productdetails.infrastructure.client.dto.ProductAvailabilityPayload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class AvailabilityClientHttp(
    properties: AvailabilityClientProperties
) : AvailabilityClient {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .build()

    override fun findByProductIdAndMarket(productId: ProductId, market: Market): ProductAvailabilityPayload? {
        return try {
            restClient.get()
                .uri("/availability/{id}?market={market}", productId.value, market.code)
                .retrieve()
                .body(ProductAvailabilityPayload::class.java)
        } catch (ex: RestClientResponseException) {
            log.warn("Failed to fetch availability [id=${productId.value}, market=${market.code}]: ${ex.statusCode}")
            null
        }
    }
}

