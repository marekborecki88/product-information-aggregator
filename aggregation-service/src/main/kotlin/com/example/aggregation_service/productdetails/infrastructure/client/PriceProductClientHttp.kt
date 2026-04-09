package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.api.dto.PricingResult
import com.example.aggregation_service.productdetails.api.dto.PricingResult.Known
import com.example.aggregation_service.productdetails.api.dto.PricingResult.Unknown
import com.example.aggregation_service.productdetails.api.dto.UnknownReason.*
import com.example.aggregation_service.productdetails.application.port.out.PricingClient
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

private const val METRIC_PRICING_CLIENT_REQUEST = "pricing.client.request"

@Component
class PriceProductClientHttp(
    @Qualifier("http-client.pricing") private val properties: HttpClientProperties,
    private val meterRegistry: MeterRegistry
) : PricingClient, HttpClient() {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = createRestClient(properties)

    override suspend fun findByProductIdAndMarket(productId: Int, market: Market, customerId: Int?): PricingResult =
        withContext(Dispatchers.IO) {
            val sample = Timer.start(meterRegistry)

            val result = fetchPrice(productId, market, customerId)
            when (result) {
                is Known -> {
                    sample.stop(pricingTimer(outcome = "success"))
                    log.debug("Price fetched successfully [productId=${productId}, market=${market.code}, customerId=${customerId}]")
                }

                is Unknown -> {
                    sample.stop(pricingTimer(outcome = result.reason.metricTag()))
                    when (result.reason) {
                        NOT_FOUND -> log.warn("No price found for product [productId=${productId}, market=${market.code}")
                        UPSTREAM_ERROR -> log.error("Pricing service returned server error [productId=${productId}, market=${market.code}, customerId=${customerId}")
                        UPSTREAM_TIMEOUT -> log.warn("Pricing service request timed out [productId=${productId}, market=${market.code}, customerId=${customerId}]")
                        UPSTREAM_UNAVAILABLE -> log.error("Pricing service is unreachable")
                    }
                }
            }
            result
        }

    private suspend fun fetchPrice(productId: Int, market: Market, customerId: Int?): PricingResult =
        try {
            withTimeout(properties.timeout) {
                restClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.path("/prices/{id}")
                            .queryParam("market", market.code)
                            .apply { if (customerId != null) queryParam("customerId", customerId) }
                            .build(productId)
                    }
                    .retrieve()
                    .body(Known::class.java)
            } ?: Unknown(UPSTREAM_ERROR)
        } catch (_: TimeoutCancellationException) {
            Unknown(UPSTREAM_TIMEOUT)
        } catch (ex: RestClientResponseException) {
            val reason = if (ex.statusCode.value() == 404) NOT_FOUND else UPSTREAM_ERROR
            Unknown(reason)
        } catch (ex: ResourceAccessException) {
            val reason = if (ex.cause is SocketTimeoutException) UPSTREAM_TIMEOUT else UPSTREAM_UNAVAILABLE
            Unknown(reason)
        }


    private fun pricingTimer(outcome: String): Timer =
        Timer.builder(METRIC_PRICING_CLIENT_REQUEST)
            .tag(TAG_OUTCOME, outcome)
            .register(meterRegistry)
}
