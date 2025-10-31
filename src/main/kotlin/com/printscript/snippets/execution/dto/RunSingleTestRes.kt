package com.printscript.snippets.execution.dto

data class RunSingleTestRes(
    val status: String,
    val actual: List<String>? = null,
    val mismatchAt: Int? = null,
    val diagnostic: DiagnosticDto? = null,
)
