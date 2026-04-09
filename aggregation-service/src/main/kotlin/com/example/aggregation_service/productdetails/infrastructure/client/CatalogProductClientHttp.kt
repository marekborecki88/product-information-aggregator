package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.api.dto.CatalogProductResult
import com.example.aggregation_service.productdetails.api.dto.CatalogProductResult.Known
import com.example.aggregation_service.productdetails.api.dto.CatalogProductResult.Unknown
import com.example.aggregation_service.productdetails.api.dto.UnknownReason.*
import com.example.aggregation_service.productdetails.application.port.out.CatalogProductClient
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.infrastructure.client.config.HttpClientProperties
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import java.net.SocketTimeoutException

private const val METRIC_CATALOG_CLIENT_REQUEST = "catalog.client.request"

@Component
class CatalogProductClientHttp(
    @Qualifier("http-client.catalog") private val properties: HttpClientProperties,
    private val meterRegistry: MeterRegistry
) : CatalogProductClient, HttpClient() {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = createRestClient(properties)

    override suspend fun findByProductIdAndMarket(productId: Int, market: Market): CatalogProductResult =
        withContext(Dispatchers.IO) {
            val sample = Timer.start(meterRegistry)

            val result = fetchCatalogProduct(productId, market)
            when (result) {
                is Known -> {
                    sample.stop(catalogTimer(outcome = "success"))
                    log.debug("Catalog product fetched successfully [id={}, market={}]", productId, market.code)
                }

                is Unknown -> {
                    sample.stop(catalogTimer(outcome = result.reason.metricTag()))
                    when (result.reason) {
                        NOT_FOUND -> log.warn("Catalog product not found [id={}, market={}]", productId, market.code)
                        UPSTREAM_ERROR -> log.error("Catalog service returned server error [id={}, market={}]", productId, market.code)
                        UPSTREAM_TIMEOUT -> log.warn("Catalog service request timed out [id={}, market={}]", productId, market.code)
                        UPSTREAM_UNAVAILABLE -> log.error("Catalog service is unreachable")
                    }
                }
            }
            result
        }

    private suspend fun fetchCatalogProduct(productId: Int, market: Market): CatalogProductResult =
        try {
            withTimeout(properties.timeout) {
                restClient.get()
                    .uri("/catalog/products/{id}?market={market}", productId, market.code)
                    .retrieve()
                    .body(Known::class.java)
            } ?: Unknown(UPSTREAM_ERROR)
        } catch (_: TimeoutCancellationException) {
            Unknown(UPSTREAM_TIMEOUT)
        } catch (ex: RestClientResponseException) {
            val reason = if (ex.statusCode.value() == 404) NOT_FOUND else UPSTREAM_ERROR
            Unknown(reason, ex.statusCode.value())
        } catch (ex: ResourceAccessException) {
            val reason = if (ex.cause is SocketTimeoutException) UPSTREAM_TIMEOUT else UPSTREAM_UNAVAILABLE
            Unknown(reason)
        }



    private fun catalogTimer(outcome: String): Timer =
        Timer.builder(METRIC_CATALOG_CLIENT_REQUEST)
            .tag(TAG_OUTCOME, outcome)
            .register(meterRegistry)
}

