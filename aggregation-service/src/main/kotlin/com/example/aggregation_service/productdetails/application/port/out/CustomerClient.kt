package com.example.aggregation_service.productdetails.application.port.out

import com.example.aggregation_service.productdetails.infrastructure.client.dto.CustomerPayload

interface CustomerClient {
    fun findByCustomerId(customerId: Int): CustomerPayload?
}

