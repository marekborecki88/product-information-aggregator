package com.example.aggregation_service.productdetails.application.port.out

import com.example.aggregation_service.productdetails.api.dto.AvailabilityResult
import com.example.aggregation_service.productdetails.domain.valueobject.Market

interface AvailabilityClient {
    suspend fun findByProductIdAndMarket(productId: Int, market: Market): AvailabilityResult
}

