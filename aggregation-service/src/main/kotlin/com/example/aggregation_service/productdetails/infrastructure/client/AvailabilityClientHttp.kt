package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.api.dto.AvailabilityResult
import com.example.aggregation_service.productdetails.api.dto.AvailabilityResult.Known
import com.example.aggregation_service.productdetails.api.dto.AvailabilityResult.Unknown
import com.example.aggregation_service.productdetails.api.dto.UnknownReason.NOT_FOUND
import com.example.aggregation_service.productdetails.api.dto.UnknownReason.UPSTREAM_ERROR
import com.example.aggregation_service.productdetails.api.dto.UnknownReason.UPSTREAM_TIMEOUT
import com.example.aggregation_service.productdetails.api.dto.UnknownReason.UPSTREAM_UNAVAILABLE
import com.example.aggregation_service.productdetails.application.port.out.AvailabilityClient
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

private const val METRIC_AVAILABILITY_CLIENT = "availability.client.request"

@Component
class AvailabilityClientHttp(
    @Qualifier("http-client.availability") private val properties: HttpClientProperties,
    private val meterRegistry: MeterRegistry
) : AvailabilityClient, HttpClient() {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = createRestClient(properties)

    override suspend fun findByProductIdAndMarket(productId: Int, market: Market): AvailabilityResult =
        withContext(Dispatchers.IO) {
            val sample = Timer.start(meterRegistry)

            val result = fetchAvailability(productId, market)
            when (result) {
                is Known -> {
                    sample.stop(timer("success"))
                    log.debug("Availability fetched [productId=${productId}, market=${market.code}]")
                }
                is Unknown -> {
                    sample.stop(timer(result.reason.metricTag()))
                    when (result.reason) {
                        NOT_FOUND -> log.warn("No availability found for productId=${productId}, market=${market.code}")
                        UPSTREAM_ERROR -> log.error("Availability service returned server error  [productId=${productId}, market=${market.code}")
                        UPSTREAM_TIMEOUT -> log.warn("Availability service request timed out [productId=${productId}, market=${market.code}")
                        UPSTREAM_UNAVAILABLE -> log.error("Availability service is unreachable")
                    }
                }
            }
            result
        }

    private suspend fun fetchAvailability(productId: Int, market: Market): AvailabilityResult =
        try {
            withTimeout(properties.timeout) {
                restClient.get()
                    .uri { it.path("/availability/{id}").queryParam("market", market.code).build(productId) }
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


    private fun timer(outcome: String): Timer =
        Timer.builder(METRIC_AVAILABILITY_CLIENT)
            .tag(TAG_OUTCOME, outcome)
            .register(meterRegistry)
}

