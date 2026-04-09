package com.example.aggregation_service.productdetails.infrastructure.client

import com.example.aggregation_service.productdetails.infrastructure.client.config.HttpClientProperties
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.util.Timeout
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient

abstract class HttpClient {
    private fun buildRequestFactory(properties: HttpClientProperties): HttpComponentsClientHttpRequestFactory {
        val connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(properties.connectTimeout.toMillis()))
            .build()
        val connectionManager = PoolingHttpClientConnectionManager().apply {
            setDefaultConnectionConfig(connectionConfig)
        }
        val requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(properties.connectionRequestTimeout.toMillis()))
            .setResponseTimeout(Timeout.ofMilliseconds(properties.readTimeout.toMillis()))
            .build()
        val httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build()
        return HttpComponentsClientHttpRequestFactory(httpClient)
    }

    protected fun createRestClient(properties: HttpClientProperties): RestClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .requestFactory(buildRequestFactory(properties))
        .build()

}
