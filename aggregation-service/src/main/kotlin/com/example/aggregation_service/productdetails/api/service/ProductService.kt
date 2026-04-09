package com.example.aggregation_service.productdetails.api.service

import com.example.aggregation_service.productdetails.api.dto.AvailabilityResult
import com.example.aggregation_service.productdetails.api.dto.PricingResult
import com.example.aggregation_service.productdetails.api.dto.PricingUnknownReason
import com.example.aggregation_service.productdetails.api.dto.ProductResponse
import com.example.aggregation_service.productdetails.api.exception.ProductNotFoundException
import com.example.aggregation_service.productdetails.api.exception.RequiredUpstreamHttpException
import com.example.aggregation_service.productdetails.api.exception.RequiredUpstreamTimeoutException
import com.example.aggregation_service.productdetails.api.exception.RequiredUpstreamUnavailableException
import com.example.aggregation_service.productdetails.application.port.out.AvailabilityClient
import com.example.aggregation_service.productdetails.application.port.out.CatalogProductClient
import com.example.aggregation_service.productdetails.application.port.out.CustomerClient
import com.example.aggregation_service.productdetails.application.port.out.PricingClient
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.ProductId
import com.example.aggregation_service.productdetails.infrastructure.client.dto.CatalogFetchResult
import com.example.aggregation_service.productdetails.infrastructure.client.dto.CustomerLookupResult
import com.example.aggregation_service.productdetails.infrastructure.client.dto.ResolvedCustomerContext
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    suspend fun findProductById(productId: Int, market: Market, customerId: Int?): ProductResponse =
        coroutineScope {
            val pid = ProductId(productId.toString())

            // Wszystkie 4 klienty odpalone równolegle — każdy na swoim włóknie
            val catalogDeferred      = async { catalogProductClient.findByProductIdAndMarket(pid, market) }
            val pricingDeferred      = async { pricingClient.findByProductIdAndMarket(pid, market, customerId) }
            val availabilityDeferred = async { availabilityClient.findByProductIdAndMarket(pid, market) }
            val customerDeferred     = async { resolveCustomerContext(customerId) }

            val catalogProduct = when (val result = catalogDeferred.await()) {
                is CatalogFetchResult.Found -> result.product
                CatalogFetchResult.NotFound ->
                    throw ProductNotFoundException(productId, market.code)
                CatalogFetchResult.Timeout ->
                    throw RequiredUpstreamTimeoutException("catalog", productId, market.code)
                CatalogFetchResult.Unavailable ->
                    throw RequiredUpstreamUnavailableException("catalog", productId, market.code)
                is CatalogFetchResult.HttpError ->
                    throw RequiredUpstreamHttpException("catalog", result.status, productId, market.code)
            }

            ProductResponse(
                id = productId,
                details = catalogProduct,
                // Constraint 2: Pricing failure -> PriceStatus.Unavailable  (obsluzone w adapterze)
                pricing = pricingDeferred.await(),
                // Constraint 3: Availability failure -> AvailabilityStatus.Unknown  (obsluzone w adapterze)
                availability = availabilityDeferred.await(),
                // Constraint 4: Customer failure -> non-personalized  (obsluzone w resolveCustomerContext)
                personalization = customerDeferred.await()
            )
        }

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