package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.api.dto.PricingResult
import com.example.aggregation_service.productdetails.api.dto.PricingUnknownReason
import com.example.aggregation_service.productdetails.application.port.out.PricingClient
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.ProductId
import com.example.aggregation_service.productdetails.infrastructure.client.config.PriceClientProperties
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class PriceProductClientHttp(
    properties: PriceClientProperties
) : PricingClient {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeout)
            setReadTimeout(properties.readTimeout)
        })
        .build()

    override fun findByProductIdAndMarket(productId: ProductId, market: Market, customerId: Int?): PricingResult {
        try {
            val x = restClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/prices/{id}")
                        .queryParam("market", market.code)
                        .apply { if (customerId != null) queryParam("customerId", customerId) }
                        .build(productId.value)
                }
                .retrieve()
                .body(PricingResult.Available::class.java)

            return x ?: PricingResult.Unavailable(PricingUnknownReason.NO_PRICE_FOR_MARKET)
        } catch (ex: RestClientResponseException) {
            println("Failed to fetch price [id=${productId.value}, market=${market.code}]: ${ex.statusCode}")
            log.warn("Failed to fetch price [id=${productId.value}, market=${market.code}]: ${ex.statusCode}")
            return PricingResult.Unavailable(PricingUnknownReason.NO_PRICE_FOR_MARKET)
        } catch (ex: ResourceAccessException) {
            println(ex.message)
            println("Failed to fetch price [id=${productId.value}, market=${market.code}]: ${ex.message}")
            log.warn("Failed to fetch price [id=${productId.value}, market=${market.code}]: ${ex.message}")
            return PricingResult.Unavailable(PricingUnknownReason.UPSTREAM_UNAVAILABLE)
        }
    }
}
