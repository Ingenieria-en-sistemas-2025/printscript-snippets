package com.printscript.tests.error
import org.springframework.http.HttpStatus

open class ApiException(val error: ApiError, val status: HttpStatus) : RuntimeException(error.message)

class NotFound(msg: String) :
    ApiException(ApiError("NOT_FOUND", msg), HttpStatus.NOT_FOUND)

class UnsupportedOperation(msg: String) :
    ApiException(ApiError("UNSUPPORTED_OPERATION", msg), HttpStatus.NOT_IMPLEMENTED)

class RunTimeError(msg: String) :
    ApiException(ApiError("RUNTIME_ERROR", msg), HttpStatus.UNPROCESSABLE_ENTITY)
