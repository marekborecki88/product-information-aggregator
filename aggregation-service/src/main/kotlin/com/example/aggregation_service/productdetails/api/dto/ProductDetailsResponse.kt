package com.example.aggregation_service.productdetails.api.dto

import com.example.aggregation_service.productdetails.domain.valueobject.Market
import com.example.aggregation_service.productdetails.domain.valueobject.Money

data class ProductDetailsResponse(
    val id: Int,
    val name: String,
    val price: Money,
    val market: Market,
    val customerId: Int?) {

}
