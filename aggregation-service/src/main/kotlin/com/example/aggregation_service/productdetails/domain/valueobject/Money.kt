package com.example.aggregation_service.productdetails.domain.valueobject

import java.math.BigDecimal


data class Money(val currency: String, val amount: BigDecimal)
