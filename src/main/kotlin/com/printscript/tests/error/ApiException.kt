package com.printscript.tests.error
import org.springframework.http.HttpStatus

open class ApiException(val error: ApiError, val status: HttpStatus) : RuntimeException(error.message)

class BadRequest(diagnostic: ApiDiagnostic) :
    ApiException(ApiError("VALIDATION_ERROR", diagnostic.message, diagnostic), HttpStatus.BAD_REQUEST)

class NotFound(msg: String) :
    ApiException(ApiError("NOT_FOUND", msg), HttpStatus.NOT_FOUND)

//el user no puede hacer la accion
class Forbidden(msg: String) :
    ApiException(ApiError("PERMISSION_DENIED", msg), HttpStatus.FORBIDDEN)

class Conflict(msg: String) :
    ApiException(ApiError("CONFLICT", msg), HttpStatus.CONFLICT)

class UpstreamError(service: String, status: Int, body: String?) :
    ApiException(ApiError("UPSTREAM_ERROR", "$service respondió $status", null), HttpStatus.BAD_GATEWAY)

