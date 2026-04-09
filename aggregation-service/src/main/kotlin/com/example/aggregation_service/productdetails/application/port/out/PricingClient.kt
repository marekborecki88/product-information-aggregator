package com.example.aggregation_service.productdetails.application.port.out

import com.example.aggregation_service.productdetails.api.dto.PricingResult
import com.example.aggregation_service.productdetails.domain.valueobject.Market

interface PricingClient {
    suspend fun findByProductIdAndMarket(productId: Int, market: Market, customerId: Int?): PricingResult
}

