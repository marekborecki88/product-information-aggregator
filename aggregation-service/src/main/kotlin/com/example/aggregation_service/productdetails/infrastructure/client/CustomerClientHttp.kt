package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.application.port.out.CustomerClient
import com.example.aggregation_service.productdetails.infrastructure.client.config.CustomerClientProperties
import com.example.aggregation_service.productdetails.infrastructure.client.dto.CustomerLookupResult
import com.example.aggregation_service.productdetails.infrastructure.client.dto.CustomerPayload
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.util.Timeout
import org.slf4j.LoggerFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class CustomerClientHttp(
    properties: CustomerClientProperties,
    private val meterRegistry: MeterRegistry
) : CustomerClient {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .requestFactory(buildRequestFactory(properties))
        .build()

    override fun findByCustomerId(customerId: Int): CustomerLookupResult {
        val sample = Timer.start(meterRegistry)
        var outcome = "success"
        var httpStatus = "200"

        return try {
            val body = restClient.get()
                .uri("/customer-context/{customerId}", customerId)
                .retrieve()
                .body(CustomerPayload::class.java)

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

            return when (statusCode) {
                404 -> CustomerLookupResult.NotFound
                else -> CustomerLookupResult.HttpError(statusCode)
            }
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

    private fun buildRequestFactory(properties: CustomerClientProperties): HttpComponentsClientHttpRequestFactory {
        val connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(properties.connectTimeout.toMillis()))
            .build()
        val connectionManager = PoolingHttpClientConnectionManager().apply {
            setDefaultConnectionConfig(connectionConfig)
        }
        val requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(properties.connectionRequestTimeout.toMillis()))
            .setResponseTimeout(Timeout.ofMilliseconds(properties.readTimeout.toMillis()))
            .build()
        val httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build()
        return HttpComponentsClientHttpRequestFactory(httpClient)
    }
}

