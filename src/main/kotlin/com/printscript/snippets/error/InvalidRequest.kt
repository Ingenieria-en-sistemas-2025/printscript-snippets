package com.printscript.snippets.error

import org.springframework.http.HttpStatus

class InvalidRequest(
    message: String,
) : ApiException(
    ApiError(
        code = "INVALID_REQUEST",
        message = message,
        diagnostics = emptyList(),
    ),
    HttpStatus.BAD_REQUEST,
)
