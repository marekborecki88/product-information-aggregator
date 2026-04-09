package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.api.dto.PricingResult
import com.example.aggregation_service.productdetails.api.dto.PricingUnknownReason
import com.example.aggregation_service.productdetails.application.port.out.PricingClient
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.ProductId
import com.example.aggregation_service.productdetails.infrastructure.client.config.PricingClientProperties
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.net.SocketTimeoutException

private const val METRIC_PRICING_CLIENT_REQUEST = "pricing.client.request"
private const val TAG_OUTCOME = "outcome"
private const val TAG_HTTP_STATUS = "http.status"

@Component
class PriceProductClientHttp(
    private val properties: PricingClientProperties,
    private val meterRegistry: MeterRegistry
) : PricingClient {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .requestFactory(requestFactory())
        .build()

    private fun requestFactory(): ClientHttpRequestFactory = SimpleClientHttpRequestFactory().apply {
        setConnectTimeout(properties.connectTimeout)
        setReadTimeout(properties.readTimeout)
    }

    override suspend fun findByProductIdAndMarket(
        productId: ProductId,
        market: Market,
        customerId: Int?
    ): PricingResult =
        withContext(Dispatchers.IO) {
            val timerSample = Timer.start(meterRegistry)

            try {
                val result = fetchPrice(productId, market, customerId)
                when (result) {
                    is PricingResult.Available -> {
                        timerSample.stop(pricingTimer(outcome = "success", httpStatus = "200"))
                        log.debug("Price fetched successfully [productId=${productId.value}, market=${market.code}, customerId=${customerId}]")
                    }
                    is PricingResult.Unavailable -> timerSample.stop(pricingTimer(outcome = result.reason.metricTag(), httpStatus = "none"))
                }
                result
            } catch (ex: RestClientResponseException) {
                resolveHttpError(ex, productId, market, customerId, timerSample)
            } catch (ex: ResourceAccessException) {
                resolveConnectivityError(ex, productId, market, customerId, timerSample)
            }
        }

    private fun PricingUnknownReason.metricTag(): String = when (this) {
        PricingUnknownReason.NO_PRICE_FOR_MARKET -> "no_price"
        PricingUnknownReason.UPSTREAM_TIMEOUT -> "timeout"
        PricingUnknownReason.UPSTREAM_UNAVAILABLE -> "unavailable"
        PricingUnknownReason.UPSTREAM_ERROR -> "upstream_error"
    }

    private suspend fun fetchPrice(productId: ProductId, market: Market, customerId: Int?): PricingResult =
        withTimeoutOrNull(properties.timeout) {
            restClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/prices/{id}")
                        .queryParam("market", market.code)
                        .apply { if (customerId != null) queryParam("customerId", customerId) }
                        .build(productId.value)
                }
                .retrieve()
                .body(PricingResult.Available::class.java)
        } ?: PricingResult.Unavailable(PricingUnknownReason.UPSTREAM_TIMEOUT)


    private fun resolveHttpError(
        ex: RestClientResponseException,
        productId: ProductId,
        market: Market,
        customerId: Int?,
        timerSample: Timer.Sample
    ): PricingResult {
        val httpStatus = ex.statusCode.value()
        return when {
            httpStatus == 404 -> {
                // 404 is a first-class business signal: the pricing service explicitly has no
                // entry for this product+market combination.
                log.info(
                    "No price found for product [productId={}, market={}, customerId={}, httpStatus={}]",
                    productId.value, market.code, customerId, httpStatus
                )
                timerSample.stop(pricingTimer(outcome = "no_price", httpStatus = httpStatus.toString()))
                PricingResult.Unavailable(PricingUnknownReason.NO_PRICE_FOR_MARKET)
            }

            httpStatus >= 500 -> {
                // 5xx: the pricing service is up but broken — an upstream error, not a missing price.
                log.error(
                    "Pricing service returned server error [productId={}, market={}, customerId={}, httpStatus={}]",
                    productId.value, market.code, customerId, httpStatus, ex
                )
                timerSample.stop(pricingTimer(outcome = "http_error", httpStatus = httpStatus.toString()))
                PricingResult.Unavailable(PricingUnknownReason.UPSTREAM_ERROR)
            }

            else -> {
                // Other 4xx (400 Bad Request, 401 Unauthorized, 403 Forbidden, etc.):
                // These signal a client-side integration problem (malformed request, missing/invalid
                // auth token, ACL mismatch) — NOT a valid "no price" business case. Mapping to
                // UPSTREAM_ERROR keeps these visible in monitoring and alerts, without polluting
                // the NO_PRICE_FOR_MARKET signal used for downstream rendering decisions.
                log.error(
                    "Pricing service returned unexpected client error [productId={}, market={}, customerId={}, httpStatus={}]",
                    productId.value, market.code, customerId, httpStatus, ex
                )
                timerSample.stop(pricingTimer(outcome = "http_error", httpStatus = httpStatus.toString()))
                PricingResult.Unavailable(PricingUnknownReason.UPSTREAM_ERROR)
            }
        }
    }

    private fun resolveConnectivityError(
        ex: ResourceAccessException,
        productId: ProductId,
        market: Market,
        customerId: Int?,
        timerSample: Timer.Sample
    ): PricingResult {
        return if (ex.cause is SocketTimeoutException) {
            timerSample.stop(pricingTimer(outcome = "timeout", httpStatus = "none"))
            log.warn(
                "Pricing service request timed out [productId={}, market={}, customerId={}]",
                productId.value, market.code, customerId, ex
            )
            PricingResult.Unavailable(PricingUnknownReason.UPSTREAM_TIMEOUT)
        } else {
            timerSample.stop(pricingTimer(outcome = "unavailable", httpStatus = "none"))
            log.warn(
                "Pricing service is unreachable [productId={}, market={}, customerId={}, cause={}]",
                productId.value, market.code, customerId, ex.message, ex
            )
            PricingResult.Unavailable(PricingUnknownReason.UPSTREAM_UNAVAILABLE)
        }
    }

    private fun pricingTimer(outcome: String, httpStatus: String): Timer =
        Timer.builder(METRIC_PRICING_CLIENT_REQUEST)
            .tag(TAG_OUTCOME, outcome)
            .tag(TAG_HTTP_STATUS, httpStatus)
            .register(meterRegistry)
}
