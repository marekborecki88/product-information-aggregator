package com.example.aggregation_service.productdetails.application.port.out

import com.example.aggregation_service.productdetails.infrastructure.client.dto.CustomerLookupResult

interface CustomerClient {
    fun findByCustomerId(customerId: Int): CustomerLookupResult
}

