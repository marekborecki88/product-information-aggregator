package com.example.aggregation_service.productdetails.application.port.out

import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.api.dto.CatalogProductResult

interface CatalogProductClient {
    suspend fun findByProductIdAndMarket(productId: Int, market: Market): CatalogProductResult
}

