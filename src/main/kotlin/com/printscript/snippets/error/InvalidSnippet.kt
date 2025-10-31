package com.printscript.snippets.error

import org.springframework.http.HttpStatus

class InvalidSnippet(
    diagnostics: List<ApiDiagnostic>,
) : ApiException(
    ApiError(
        code = "INVALID_SNIPPET",
        message = "El snippet no es válido",
        diagnostics = diagnostics,
    ),
    HttpStatus.BAD_REQUEST,
)
