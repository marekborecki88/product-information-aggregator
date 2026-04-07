package com.example.aggregation_service.productdetails.api.service

import com.example.aggregation_service.productdetails.api.dto.ProductDetailsResponse
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.Money
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class ProductDetailsService {
    fun findProductById(productId: Int, market: Market, customerId: Int?): ProductDetailsResponse {

        return ProductDetailsResponse(
            id = productId,
            name = "Product $productId",
            price = Money(currency = "Euro", amount = BigDecimal.valueOf(100)),
            market = market,
            customerId = customerId
        )
    }
}