package com.example.aggregation_service.productdetails.api.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException::class)
    fun handleNotFound(ex: ProductNotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                ApiError(
                    code = "PRODUCT_NOT_FOUND",
                    message = ex.message ?: "Product not found"
                )
            )

    @ExceptionHandler(RequiredUpstreamTimeoutException::class)
    fun handleRequiredTimeout(ex: RequiredUpstreamTimeoutException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
            .body(
                ApiError(
                    code = "REQUIRED_UPSTREAM_TIMEOUT",
                    message = ex.message ?: "Required upstream timed out"
                )
            )

    @ExceptionHandler(RequiredUpstreamUnavailableException::class)
    fun handleRequiredUnavailable(ex: RequiredUpstreamUnavailableException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(
                ApiError(
                    code = "REQUIRED_UPSTREAM_UNAVAILABLE",
                    message = ex.message ?: "Required upstream unavailable"
                )
            )
}

data class ApiError(
    val code: String,
    val message: String
)