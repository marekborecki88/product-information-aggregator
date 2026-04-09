package com.example.aggregation_service.productdetails.api.controller

import com.example.aggregation_service.productdetails.api.dto.ProductResponse
import com.example.aggregation_service.productdetails.api.service.ProductService
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ProductController(val productService: ProductService) {
    @GetMapping("/products/{productId}")
    suspend fun getProductById(
        @PathVariable("productId") productId: Int,
        @RequestParam(value = "market", required = true) market: Market,
        @RequestParam(value = "customerId", required = false) customerId: Int?
    ): ProductResponse = productService.findProductById(productId, market, customerId)

}