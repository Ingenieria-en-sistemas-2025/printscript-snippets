package com.printscript.tests.error

data class ApiError(
    val code: String,
    val message: String, // legible para la UI
    val diagnostics: List<ApiDiagnostic>? = null, // en errores de parse o linter
)
