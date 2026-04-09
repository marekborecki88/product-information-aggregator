package com.example.aggregation_service.productdetails.api.service

import com.example.aggregation_service.productdetails.api.dto.UnknownReason
import com.example.aggregation_service.productdetails.application.port.out.CustomerClient
import com.example.aggregation_service.productdetails.api.dto.CustomerResult
import com.example.aggregation_service.productdetails.api.dto.ResolvedCustomerContext
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service

@Service
class CustomerService(
    private val customerClient: CustomerClient,
    private val meterRegistry: MeterRegistry
) {
    suspend fun resolveCustomerContext(customerId: Int?): ResolvedCustomerContext {
        if (customerId == null) {
            meterRegistry.counter(
                "product_aggregator.customer_context.fallback",
                "reason", "no_customer_id"
            ).increment()

            return ResolvedCustomerContext(
                segment = "STANDARD",
                preferences = emptyList(),
                personalized = false,
                responseDegraded = false
            )
        }
        val result = customerClient.findByCustomerId(customerId)
        return when (result) {
            is CustomerResult.Available -> ResolvedCustomerContext(
                segment = result.segment,
                preferences = result.preferences,
                personalized = true,
                responseDegraded = false
            )
            is CustomerResult.Unavailable -> {
                when(result.reason) {
                    UnknownReason.NOT_FOUND -> {
                        meterRegistry.counter(
                            "product_aggregator.customer_context.fallback",
                            "reason", "not_found"
                        ).increment()
                    }
                    UnknownReason.UPSTREAM_TIMEOUT  -> {
                        meterRegistry.counter(
                            "product_aggregator.customer_context.fallback",
                            "reason", "timeout"
                        ).increment()

                    }
                    UnknownReason.UPSTREAM_ERROR -> {
                        meterRegistry.counter(
                            "product_aggregator.customer_context.fallback",
                            "reason", "http_error"
                        ).increment()

                    }
                    UnknownReason.UPSTREAM_UNAVAILABLE -> {
                        meterRegistry.counter(
                            "product_aggregator.customer_context.fallback",
                            "reason", "unavailable"
                        ).increment()
                    }
                }
                ResolvedCustomerContext("STANDARD", emptyList(), personalized = false, responseDegraded = true)
            }
        }
    }

}