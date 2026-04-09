package com.example.aggregation_service.productdetails.api.service

import com.example.aggregation_service.productdetails.api.dto.ProductResponse
import com.example.aggregation_service.productdetails.api.exception.ProductNotFoundException
import com.example.aggregation_service.productdetails.api.exception.RequiredUpstreamHttpException
import com.example.aggregation_service.productdetails.api.exception.RequiredUpstreamTimeoutException
import com.example.aggregation_service.productdetails.api.exception.RequiredUpstreamUnavailableException
import com.example.aggregation_service.productdetails.application.port.out.AvailabilityClient
import com.example.aggregation_service.productdetails.application.port.out.CatalogProductClient
import com.example.aggregation_service.productdetails.application.port.out.PricingClient
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.api.dto.CatalogProductResult
import com.example.aggregation_service.productdetails.api.dto.UnknownReason
import com.example.aggregation_service.productdetails.api.dto.UnknownReason.NOT_FOUND
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service

@Service
class ProductService(
    private val catalogProductClient: CatalogProductClient,
    private val pricingClient: PricingClient,
    private val availabilityClient: AvailabilityClient,
    private val customerService: CustomerService
) {
    suspend fun findProductById(productId: Int, market: Market, customerId: Int?): ProductResponse =
        coroutineScope {

            val catalogDeferred      = async { catalogProductClient.findByProductIdAndMarket(productId, market) }
            val pricingDeferred      = async { pricingClient.findByProductIdAndMarket(productId, market, customerId) }
            val availabilityDeferred = async { availabilityClient.findByProductIdAndMarket(productId, market) }
            val customerDeferred     = async { customerService.resolveCustomerContext(customerId) }

            val catalogProduct = when (val result = catalogDeferred.await()) {
                is CatalogProductResult.Known -> result
                is CatalogProductResult.Unknown -> {
                    when (result.reason) {
                        NOT_FOUND -> throw ProductNotFoundException(productId, market.code)
                        UnknownReason.UPSTREAM_TIMEOUT -> throw RequiredUpstreamTimeoutException("catalog", productId, market.code)
                        UnknownReason.UPSTREAM_UNAVAILABLE -> throw RequiredUpstreamUnavailableException("catalog", productId, market.code)
                        UnknownReason.UPSTREAM_ERROR -> throw RequiredUpstreamHttpException("catalog", result.statusCode, productId, market.code)
                    }
                }
            }

            ProductResponse(
                id = productId,
                details = catalogProduct,
                pricing = pricingDeferred.await(),
                availability = availabilityDeferred.await(),
                personalization = customerDeferred.await()
            )
        }

}