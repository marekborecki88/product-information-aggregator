package com.example.aggregation_service.productdetails.api.exception

class ProductNotFoundException(
    val productId: Int,
    val market: String
) : RuntimeException("Product $productId not found in market $market")