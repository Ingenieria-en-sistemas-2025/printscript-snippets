package com.printscript.snippets.error

data class ApiError(
    val code: String,
    val message: String, // legible para la UI
    val diagnostics: List<ApiDiagnostic>? = null, // en errores de parse o linter
)
