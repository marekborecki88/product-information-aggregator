package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.api.dto.AvailabilityResult
import com.example.aggregation_service.productdetails.api.dto.AvailabilityUnknownReason
import com.example.aggregation_service.productdetails.application.port.out.AvailabilityClient
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.ProductId
import com.example.aggregation_service.productdetails.infrastructure.client.config.AvailabilityClientProperties
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
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
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(properties.readTimeout)
        })
        .build()

    override fun findByProductIdAndMarket(productId: ProductId, market: Market): AvailabilityResult {
        try {
            return restClient.get()
                .uri("/availability/{id}?market={market}", productId.value, market.code)
                .retrieve()
                .body(AvailabilityResult.Known::class.java) ?: AvailabilityResult.Unknown(AvailabilityUnknownReason.UPSTREAM_SERVICE_ERROR)
        } catch (ex: RestClientResponseException) {
            log.warn("Failed to fetch availability [id=${productId.value}, market=${market.code}]: ${ex.statusCode}")
            return AvailabilityResult.Unknown(AvailabilityUnknownReason.UPSTREAM_SERVICE_ERROR)
        }
    }
}

