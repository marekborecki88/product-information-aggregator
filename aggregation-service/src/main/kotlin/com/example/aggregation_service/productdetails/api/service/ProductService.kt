package com.example.aggregation_service.productdetails.api.service

import com.example.aggregation_service.productdetails.api.dto.AvailabilityResult
import com.example.aggregation_service.productdetails.api.dto.PricingResult
import com.example.aggregation_service.productdetails.api.dto.PricingUnknownReason
import com.example.aggregation_service.productdetails.api.dto.ProductResponse
import com.example.aggregation_service.productdetails.application.port.out.AvailabilityClient
import com.example.aggregation_service.productdetails.application.port.out.CatalogProductClient
import com.example.aggregation_service.productdetails.application.port.out.CustomerClient
import com.example.aggregation_service.productdetails.application.port.out.PricingClient
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.ProductId
import com.example.aggregation_service.productdetails.infrastructure.client.dto.CustomerLookupResult
import com.example.aggregation_service.productdetails.infrastructure.client.dto.ResolvedCustomerContext
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProductService(
    private val catalogProductClient: CatalogProductClient,
    private val pricingClient: PricingClient,
    private val availabilityClient: AvailabilityClient,
    private val customerClient: CustomerClient,
    private val meterRegistry: MeterRegistry
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun findProductById(productId: Int, market: Market, customerId: Int?): ProductResponse {
        // Constraint 1: Catalog failure fails the entire request
        val catalogProduct = catalogProductClient.findByProductIdAndMarket(
            productId = ProductId(productId.toString()),
            market = market
        ) ?: throw NoSuchElementException("Product $productId not found in market ${market.code}")

        // Constraint 2: Pricing failure → PriceStatus.Unavailable
        val pricing: PricingResult = pricingClient.findByProductIdAndMarket(
                productId = ProductId(productId.toString()),
                market = market,
                customerId = customerId
            )

        // Constraint 3: Availability failure → AvailabilityStatus.Unknown
        val availabilityStatus: AvailabilityResult = availabilityClient.findByProductIdAndMarket(
                productId = ProductId(productId.toString()),
                market = market
            )

        // Constraint 4: Customer failure (or no customerId) → non-personalized response
        val customerPayload = resolveCustomerContext(customerId)

        return ProductResponse(
            id = productId,
            details = catalogProduct,
            pricing = pricing,
            availability = availabilityStatus,
            personalization = customerPayload
        )
    }

    fun resolveCustomerContext(customerId: Int?): ResolvedCustomerContext {
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

        return when (val result = customerClient.findByCustomerId(customerId)) {
            is CustomerLookupResult.Found -> ResolvedCustomerContext(
                segment = result.payload.segment,
                preferences = result.payload.preferences,
                personalized = true,
                responseDegraded = false
            )

            CustomerLookupResult.NotFound -> {
                meterRegistry.counter(
                    "product_aggregator.customer_context.fallback",
                    "reason", "not_found"
                ).increment()

                ResolvedCustomerContext("STANDARD", emptyList(), personalized = false, responseDegraded = true)
            }

            CustomerLookupResult.TimedOut -> {
                meterRegistry.counter(
                    "product_aggregator.customer_context.fallback",
                    "reason", "timeout"
                ).increment()

                ResolvedCustomerContext("STANDARD", emptyList(), personalized = false, responseDegraded = true)
            }

            is CustomerLookupResult.HttpError -> {
                meterRegistry.counter(
                    "product_aggregator.customer_context.fallback",
                    "reason", "http_error"
                ).increment()

                ResolvedCustomerContext("STANDARD", emptyList(), personalized = false, responseDegraded = true)
            }

            CustomerLookupResult.NoCustomerId -> ResolvedCustomerContext(
                "STANDARD", emptyList(), personalized = false, responseDegraded = false
            )
        }
    }
}