package com.example.aggregation_service.productdetails.infrastructure.client.config
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.util.Timeout
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient
@Configuration
class AvailabilityClientConfig(
    private val properties: AvailabilityClientProperties
) {
    @Bean("availabilityRestClient")
    fun availabilityRestClient(): RestClient {
        val connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(properties.connectTimeout.toMillis()))
            .build()
        val connectionManager = PoolingHttpClientConnectionManager().apply {
            setDefaultConnectionConfig(connectionConfig)
        }
        val requestConfig = RequestConfig.custom()
            .setResponseTimeout(Timeout.ofMilliseconds(properties.readTimeout.toMillis()))
            .build()
        val httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build()
        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(HttpComponentsClientHttpRequestFactory(httpClient))
            .build()
    }
}
