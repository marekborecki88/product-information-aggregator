package com.example.aggregation_service.productdetails.api.converter

import com.example.aggregation_service.productdetails.domain.valueobject.Market
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class StringToMarketConverter : Converter<String, Market> {
    override fun convert(source: String): Market =
        Market.entries.firstOrNull { it.code.equals(source, ignoreCase = true) }
            ?: throw IllegalArgumentException(
                "Unknown market code '$source'. Valid values: ${Market.entries.map { it.code }}"
            )
}

