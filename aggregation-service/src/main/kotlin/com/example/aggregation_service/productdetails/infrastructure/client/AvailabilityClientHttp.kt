package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.api.dto.AvailabilityResult
import com.example.aggregation_service.productdetails.api.dto.AvailabilityUnknownReason
import com.example.aggregation_service.productdetails.application.port.out.AvailabilityClient
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.ProductId
import com.example.aggregation_service.productdetails.infrastructure.client.config.AvailabilityClientProperties
import com.example.aggregation_service.productdetails.infrastructure.client.dto.AvailabilityPayload
import com.example.aggregation_service.productdetails.infrastructure.client.dto.toResult
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.SocketTimeoutException

private const val METRIC_AVAILABILITY_CLIENT = "availability.client.request"

@Component
class AvailabilityClientHttp(
    @Qualifier("availabilityRestClient") private val restClient: RestClient,
    private val properties: AvailabilityClientProperties,
    private val meterRegistry: MeterRegistry
) : AvailabilityClient {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun findByProductIdAndMarket(productId: ProductId, market: Market): AvailabilityResult =
        withContext(Dispatchers.IO) {
            val sample = Timer.start(meterRegistry)
            try {
                val payload = fetchAvailability(productId, market)
                if (payload != null) {
                    log.debug("Availability fetched [productId={}, market={}]", productId.value, market.code)
                    sample.stop(timer("success", "200"))
                    payload.toResult()
                } else {
                    log.info(
                        "Availability service returned empty body [productId={}, market={}]",
                        productId.value, market.code
                    )
                    sample.stop(timer("empty_body", "200"))
                    AvailabilityResult.Unknown(AvailabilityUnknownReason.UPSTREAM_SERVICE_ERROR)
                }
            } catch (ex: RestClientResponseException) {
                resolveHttpError(ex, productId, market, sample)
            } catch (ex: ResourceAccessException) {
                resolveConnectivityError(ex, productId, market, sample)
            }
        }

    private suspend fun fetchAvailability(productId: ProductId, market: Market): AvailabilityPayload? =
        withTimeoutOrNull(properties.timeout) {
            restClient.get()
                .uri { it.path("/availability/{id}").queryParam("market", market.code).build(productId.value) }
                .retrieve()
                .body(AvailabilityPayload::class.java)
        } ?: run {
            log.warn("Availability client request timeout [id=${productId.value}, market=${market.code}]")
            null
        }

    private fun resolveHttpError(
        ex: RestClientResponseException,
        productId: ProductId,
        market: Market,
        sample: Timer.Sample
    ): AvailabilityResult {
        val status = ex.statusCode.value()
        return when {
            status == 404 -> {
                log.info(
                    "Availability not found [productId={}, market={}, status={}]",
                    productId.value, market.code, status
                )
                sample.stop(timer("not_found", status.toString()))
                AvailabilityResult.Unknown(AvailabilityUnknownReason.UPSTREAM_SERVICE_ERROR)
            }
            status >= 500 -> {
                log.error(
                    "Availability service server error [productId={}, market={}, status={}]",
                    productId.value, market.code, status, ex
                )
                sample.stop(timer("http_error", status.toString()))
                AvailabilityResult.Unknown(AvailabilityUnknownReason.UPSTREAM_SERVICE_ERROR)
            }
            else -> {
                log.error(
                    "Availability service unexpected client error [productId={}, market={}, status={}]",
                    productId.value, market.code, status, ex
                )
                sample.stop(timer("http_error", status.toString()))
                AvailabilityResult.Unknown(AvailabilityUnknownReason.UPSTREAM_SERVICE_ERROR)
            }
        }
    }

    private fun resolveConnectivityError(
        ex: ResourceAccessException,
        productId: ProductId,
        market: Market,
        sample: Timer.Sample
    ): AvailabilityResult =
        if (ex.cause is SocketTimeoutException) {
            log.warn(
                "Availability service timed out [productId={}, market={}]",
                productId.value, market.code, ex
            )
            sample.stop(timer("timeout", "none"))
            AvailabilityResult.Unknown(AvailabilityUnknownReason.UPSTREAM_SERVICE_TIMEOUT)
        } else {
            log.warn(
                "Availability service unreachable [productId={}, market={}, cause={}]",
                productId.value, market.code, ex.message, ex
            )
            sample.stop(timer("unavailable", "none"))
            AvailabilityResult.Unknown(AvailabilityUnknownReason.UPSTREAM_SERVICE_UNAVAILABLE)
        }

    private fun timer(outcome: String, httpStatus: String): Timer =
        Timer.builder(METRIC_AVAILABILITY_CLIENT)
            .tag("outcome", outcome)
            .tag("http.status", httpStatus)
            .register(meterRegistry)
}

