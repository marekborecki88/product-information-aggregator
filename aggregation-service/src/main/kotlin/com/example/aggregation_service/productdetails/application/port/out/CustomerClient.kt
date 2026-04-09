package com.example.aggregation_service.productdetails.application.port.out

import com.example.aggregation_service.productdetails.api.dto.CustomerResult

interface CustomerClient {
    suspend fun findByCustomerId(customerId: Int): CustomerResult
}

