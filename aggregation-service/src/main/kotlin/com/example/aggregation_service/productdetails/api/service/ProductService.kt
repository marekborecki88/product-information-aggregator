package com.example.aggregation_service.productdetails.api.service

import com.example.aggregation_service.productdetails.api.dto.ProductResponse
 import com.example.aggregation_service.productdetails.application.port.out.AvailabilityClient
import com.example.aggregation_service.productdetails.application.port.out.CatalogProductClient
import com.example.aggregation_service.productdetails.application.port.out.PricingClient
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.ProductId
import org.springframework.stereotype.Service

@Service
class ProductService(
    private val catalogProductClient: CatalogProductClient,
    private val pricingClient: PricingClient,
    private val availabilityClient: AvailabilityClient
) {
    fun findProductById(productId: Int, market: Market, customerId: Int?): ProductResponse {
        val catalogProduct = catalogProductClient.findByProductIdAndMarket(
            productId = ProductId(productId.toString()),
            market = market
        ) ?: throw NoSuchElementException("Product $productId not found in market ${market.code}")

        val pricePayload = pricingClient.findByProductIdAndMarket(
            productId = ProductId(productId.toString()),
            market = market,
            customerId = customerId
        ) ?: throw NoSuchElementException("Price for product $productId not found in market ${market.code}")

        val availabilityPayload = availabilityClient.findByProductIdAndMarket(
            productId = ProductId(productId.toString()),
            market = market
        ) ?: throw NoSuchElementException("Availability for product $productId not found in market ${market.code}")

        return ProductResponse(
            id = productId,
            details = catalogProduct,
            priceInfo = pricePayload,
            availabilityInfo = availabilityPayload,
            customerId = customerId
        )
    }
}