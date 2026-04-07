package com.example.aggregation_service.productdetails.application.port.out

import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.ProductId
import com.example.aggregation_service.productdetails.infrastructure.client.dto.PricePayload

interface PricingClient {
    fun findByProductIdAndMarket(productId: ProductId, market: Market, customerId: Int?): PricePayload?
}

