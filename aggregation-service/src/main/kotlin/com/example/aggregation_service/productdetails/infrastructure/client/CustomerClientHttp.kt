package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.api.dto.UnknownReason.*
import com.example.aggregation_service.productdetails.application.port.out.CustomerClient
import com.example.aggregation_service.productdetails.infrastructure.client.config.HttpClientProperties
import com.example.aggregation_service.productdetails.api.dto.CustomerResult
import com.example.aggregation_service.productdetails.api.dto.CustomerResult.Unavailable
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

private const val METRIC_CUSTOMER_CLIENT_REQUEST = "customer.client.request"

@Component
class CustomerClientHttp(
    @Qualifier("http-client.customer") private val properties: HttpClientProperties,
    private val meterRegistry: MeterRegistry
) : CustomerClient, HttpClient() {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = createRestClient(properties)

    override suspend fun findByCustomerId(customerId: Int): CustomerResult =
        withContext(Dispatchers.IO) {
            val sample = Timer.start(meterRegistry)

            val result = fetchCustomer(customerId)
            when (result) {
                is CustomerResult.Available -> {
                    sample.stop(customerTimer(outcome = "success"))
                    log.debug("Successfully fetched customer $customerId")
                }
                is Unavailable -> {
                    sample.stop(customerTimer(outcome = result.reason.metricTag()))
                    when (result.reason) {
                        NOT_FOUND -> log.warn("Customer not found [customerId=$customerId]")
                        UPSTREAM_ERROR -> log.error("Customer service returned server error [customerId=$customerId]")
                        UPSTREAM_TIMEOUT -> log.warn("Customer service request timed out [customerId=$customerId]")
                        UPSTREAM_UNAVAILABLE -> log.error("Customer service is unreachable")
                    }
                }
            }
            result
        }

    private suspend fun fetchCustomer(customerId: Int): CustomerResult =
        try {
            withTimeout(properties.timeout) {
                restClient.get()
                    .uri("/customer-context/{customerId}", customerId)
                    .retrieve()
                    .body(CustomerResult.Available::class.java)
            } ?: Unavailable(UPSTREAM_ERROR)
        } catch (_: TimeoutCancellationException) {
            Unavailable(UPSTREAM_TIMEOUT)
        } catch (ex: RestClientResponseException) {
            val reason = if (ex.statusCode.value() == 404) NOT_FOUND else UPSTREAM_ERROR
            Unavailable(reason)
        } catch (ex: ResourceAccessException) {
            val reason = if (ex.cause is SocketTimeoutException) UPSTREAM_TIMEOUT else UPSTREAM_UNAVAILABLE
            Unavailable(reason)
        }


    private fun customerTimer(outcome: String): Timer =
        Timer.builder(METRIC_CUSTOMER_CLIENT_REQUEST)
            .tag(TAG_OUTCOME, outcome)
            .register(meterRegistry)

}

