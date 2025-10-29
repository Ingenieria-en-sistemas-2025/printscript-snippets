package com.printscript.tests.error

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.multipart.support.MissingServletRequestPartException

@RestControllerAdvice
class RestExceptionHandler {

    //excepciones de dominio
    @ExceptionHandler(ApiException::class)
    fun handleApi(ex: ApiException): ResponseEntity<ApiError> =
        ResponseEntity.status(ex.status).body(ex.error)

    //errors de request (ej json invalido)
    @ExceptionHandler(
        IllegalArgumentException::class,
        MissingServletRequestParameterException::class,
        MissingServletRequestPartException::class,
        MethodArgumentNotValidException::class
    )
    fun badRequest(ex: Exception) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError(code = "BAD_REQUEST", message = ex.message ?: "Bad request"))

    // Si a algún cliente Remote* se le escapó un RestClientResponseException
    @ExceptionHandler(RestClientResponseException::class)
    fun upstream(ex: RestClientResponseException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ApiError(code = "UPSTREAM_ERROR", message = "Servicio externo respondió ${ex.rawStatusCode}"))

    // Fallback
    @ExceptionHandler(Exception::class)
    fun boom(ex: Exception) =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError(code = "INTERNAL_ERROR", message = ex.message ?: "Unexpected error"))
}
