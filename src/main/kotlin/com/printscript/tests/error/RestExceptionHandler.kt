package com.printscript.tests.error

import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.server.ResponseStatusException
import java.net.SocketTimeoutException

@RestControllerAdvice
class RestExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun handleApi(ex: ApiException): ResponseEntity<ApiError> =
        ResponseEntity.status(ex.status).body(ex.error)

    @ExceptionHandler(
        IllegalArgumentException::class,
        MissingServletRequestParameterException::class,
        MissingServletRequestPartException::class,
        MethodArgumentNotValidException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class
    )
    fun badRequest(ex: Exception): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError(code = "BAD_REQUEST", message = ex.message ?: "Bad request"))

    @ExceptionHandler(ConstraintViolationException::class)
    fun constraint(@Suppress("UNUSED_PARAMETER") ex: ConstraintViolationException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError(code = "CONSTRAINT_VIOLATION", message = "Request validation failed"))

    @ExceptionHandler(AccessDeniedException::class)
    fun forbidden(@Suppress("UNUSED_PARAMETER") ex: AccessDeniedException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiError(code = "FORBIDDEN", message = "Access denied"))

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun conflict(@Suppress("UNUSED_PARAMETER") ex: DataIntegrityViolationException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError(code = "DATA_INTEGRITY", message = "Integrity constraint violation"))

    @ExceptionHandler(ResponseStatusException::class)
    fun status(ex: ResponseStatusException): ResponseEntity<ApiError> =
        ResponseEntity.status(ex.statusCode)
            .body(ApiError(code = ex.statusCode.value().toString(), message = ex.reason ?: "Error"))


    @ExceptionHandler(RestClientResponseException::class)
    fun upstream(ex: RestClientResponseException): ResponseEntity<ApiError> =
        ResponseEntity.status(mapUpstreamStatus(ex))
            .body(
                ApiError(
                    code = "UPSTREAM_ERROR",
                    message = "Servicio externo respondió ${ex.statusCode.value()}"
                )
            )

    @ExceptionHandler(SocketTimeoutException::class)
    fun timeout(@Suppress("UNUSED_PARAMETER") ex: SocketTimeoutException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
            .body(ApiError(code = "UPSTREAM_TIMEOUT", message = "Timeout al llamar servicio externo"))

    @ExceptionHandler(Exception::class)
    fun boom(ex: Exception): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError(code = "INTERNAL_ERROR", message = ex.message ?: "Unexpected error"))

    private fun mapUpstreamStatus(ex: RestClientResponseException): HttpStatus =
        when (ex.statusCode.value()) {
            401 -> HttpStatus.UNAUTHORIZED
            403 -> HttpStatus.FORBIDDEN
            404 -> HttpStatus.NOT_FOUND
            409 -> HttpStatus.CONFLICT
            413 -> HttpStatus.PAYLOAD_TOO_LARGE
            in 500..599 -> HttpStatus.BAD_GATEWAY
            else -> HttpStatus.BAD_GATEWAY
        }
}
