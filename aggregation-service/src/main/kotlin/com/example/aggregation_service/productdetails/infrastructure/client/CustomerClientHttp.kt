package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.application.port.out.CustomerClient
import com.example.aggregation_service.productdetails.infrastructure.client.config.HttpClientProperties
import com.example.aggregation_service.productdetails.infrastructure.client.dto.CustomerLookupResult
import com.example.aggregation_service.productdetails.infrastructure.client.dto.CustomerPayload
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class CustomerClientHttp(
    @Qualifier("http-client.customer") private val properties: HttpClientProperties,
    private val meterRegistry: MeterRegistry
) : CustomerClient, HttpClient() {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .requestFactory(buildRequestFactory(properties))
        .build()

    override suspend fun findByCustomerId(customerId: Int): CustomerLookupResult =
        withContext(Dispatchers.IO) {
            val sample = Timer.start(meterRegistry)
            var outcome = "success"
            var httpStatus = "200"

            try {
                val body = withTimeoutOrNull(properties.timeout) {
                    restClient.get()
                        .uri("/customer-context/{customerId}", customerId)
                        .retrieve()
                        .body(CustomerPayload::class.java)
                } ?: run {
                    log.warn("Customer client request timeout customerId=$customerId")
                    null
                }

                if (body == null) {
                    outcome = "empty_body"
                    CustomerLookupResult.NotFound
                } else {
                    CustomerLookupResult.Found(body)
                }
            } catch (ex: ResourceAccessException) {
                outcome = "timeout"
                httpStatus = "n/a"
                log.warn("Timeout/network error fetching customer [customerId=$customerId]: ${ex.message}")
                CustomerLookupResult.TimedOut
            } catch (ex: RestClientResponseException) {
                val statusCode = ex.statusCode.value()
                outcome = when {
                    statusCode == 404 -> "not_found"
                    statusCode >= 500 -> "http_error_5xx"
                    else -> "http_error_4xx"
                }
                httpStatus = statusCode.toString()
                log.warn("HTTP error fetching customer [customerId=$customerId]: ${ex.statusCode}")
                if (statusCode == 404) CustomerLookupResult.NotFound
                else CustomerLookupResult.HttpError(statusCode)
            } finally {
                sample.stop(
                    meterRegistry.timer(
                        "customer.client.request",
                        "outcome", outcome,
                        "http.status", httpStatus
                    )
                )
            }
        }
}

