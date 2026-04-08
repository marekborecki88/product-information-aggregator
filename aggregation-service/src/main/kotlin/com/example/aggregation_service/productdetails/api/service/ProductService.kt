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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProductService(
    private val catalogProductClient: CatalogProductClient,
    private val pricingClient: PricingClient,
    private val availabilityClient: AvailabilityClient,
    private val customerClient: CustomerClient
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
        val customerPayload = try {
            customerId?.let { customerClient.findByCustomerId(it) }
        } catch (ex: Exception) {
            log.warn("Customer service unavailable for customerId=$customerId: ${ex.message}")
            null
        }

        return ProductResponse(
            id = productId,
            details = catalogProduct,
            pricing = pricing,
            availability = availabilityStatus,
            personalization = customerPayload
        )
    }
}