package com.example.aggregation_service.productdetails.infrastructure.client
import com.example.aggregation_service.productdetails.application.port.out.PricingClient
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.ProductId
import com.example.aggregation_service.productdetails.infrastructure.client.config.PriceClientProperties
import com.example.aggregation_service.productdetails.infrastructure.client.dto.PricePayload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
@Component
class PriceProductClientHttp(
    properties: PriceClientProperties
) : PricingClient {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .build()
    override fun findByProductIdAndMarket(productId: ProductId, market: Market, customerId: Int?): PricePayload? {
        return try {
            restClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/prices/{id}")
                        .queryParam("market", market.code)
                        .apply { if (customerId != null) queryParam("customerId", customerId) }
                        .build(productId.value)
                }
                .retrieve()
                .body(PricePayload::class.java)
        } catch (ex: RestClientResponseException) {
            log.warn("Failed to fetch price [id=${productId.value}, market=${market.code}]: ${ex.statusCode}")
            null
        }
    }
}
