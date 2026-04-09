package com.example.aggregation_service.productdetails.application.port.out

import com.example.aggregation_service.productdetails.api.dto.PricingResult
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.ProductId

interface PricingClient {
    suspend fun findByProductIdAndMarket(productId: ProductId, market: Market, customerId: Int?): PricingResult
}

