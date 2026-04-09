package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.application.port.out.CatalogProductClient
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.ProductId
import com.example.aggregation_service.productdetails.infrastructure.client.config.HttpClientProperties
import com.example.aggregation_service.productdetails.infrastructure.client.dto.CatalogFetchResult
import com.example.aggregation_service.productdetails.infrastructure.client.dto.CatalogProductPayload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException

@Component
class CatalogProductClientHttp(
    @Qualifier("http-client.catalog") private val properties: HttpClientProperties
) : CatalogProductClient, HttpClient() {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .requestFactory(buildRequestFactory(properties))
        .build()

    override suspend fun findByProductIdAndMarket(productId: ProductId, market: Market): CatalogFetchResult =
        withContext(Dispatchers.IO) {
            try {
                val payload = withTimeoutOrNull(properties.timeout) {
                    restClient.get()
                        .uri("/catalog/products/{id}?market={market}", productId.value, market.code)
                        .retrieve()
                        .body(CatalogProductPayload::class.java)
                }

                if (payload != null) {
                    CatalogFetchResult.Found(payload)
                } else {
                    log.warn("Catalog client request timeout [id={}, market={}]", productId.value, market.code)
                    CatalogFetchResult.Timeout
                }
            } catch (ex: RestClientResponseException) {
                when (ex.statusCode.value()) {
                    404 -> {
                        log.info("Catalog product not found [id={}, market={}]", productId.value, market.code)
                        CatalogFetchResult.NotFound
                    }
                    else -> {
                        log.warn(
                            "Catalog service http error [id={}, market={}, status={}]",
                            productId.value, market.code, ex.statusCode.value(), ex
                        )
                        CatalogFetchResult.HttpError(ex.statusCode.value())
                    }
                }
            } catch (ex: ResourceAccessException) {
                if (ex.cause is SocketTimeoutException) {
                    log.warn("Catalog client request timeout [id={}, market={}]", productId.value, market.code)
                    CatalogFetchResult.Timeout
                } else {
                    log.warn("Catalog server unavailable")
                    CatalogFetchResult.Unavailable
                }
            }
        }
}

