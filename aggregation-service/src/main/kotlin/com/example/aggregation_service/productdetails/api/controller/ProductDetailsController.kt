package com.example.aggregation_service.productdetails.api.controller

import com.example.aggregation_service.productdetails.api.dto.ProductDetailsResponse
import com.example.aggregation_service.productdetails.api.service.ProductDetailsService
import com.example.aggregation_service.productdetails.domain.valueobject.Market
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ProductDetailsController(val productService: ProductDetailsService) {
    @GetMapping("/products/{productId}")
    fun getProductById(
        @PathVariable("productId") productId: Int,
        @RequestParam(value = "market", required = true) market: Market,
        @RequestParam(value = "customerId", required = false) customerId: Int
    ): ProductDetailsResponse = productService.findProductById(productId, market, customerId)

}