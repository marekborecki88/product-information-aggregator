package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.application.port.out.CatalogProductClient
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.ProductId
import com.example.aggregation_service.productdetails.infrastructure.client.config.CatalogClientProperties
import com.example.aggregation_service.productdetails.infrastructure.client.dto.CatalogProductPayload
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Component
class CatalogProductClientHttp(
    private val properties: CatalogClientProperties
) : CatalogProductClient {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .build()

    override suspend fun findByProductIdAndMarket(productId: ProductId, market: Market): CatalogProductPayload? =
        withContext(Dispatchers.IO) {
            try {
                withTimeoutOrNull(properties.timeout) {
                    restClient.get()
                        .uri("/catalog/products/{id}?market={market}", productId.value, market.code)
                        .retrieve()
                        .body(CatalogProductPayload::class.java)
                } ?: run {
                    log.warn("Catalog client request timeout [id=${productId.value}, market=${market.code}]")
                    null
                }
            } catch (ex: RestClientResponseException) {
                log.warn("Failed to fetch product [id=${productId.value}, market=${market.code}]: ${ex.statusCode}")
                null
            }
        }
}

