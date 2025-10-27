package com.printscript.tests.error

data class ApiError(
    val code: String,                 // "VALIDATION_ERROR", "NOT_FOUND", etc.
    val message: String,              // legible para la UI
    val diagnostic: ApiDiagnostic? = null //en errores de parse o linter
)