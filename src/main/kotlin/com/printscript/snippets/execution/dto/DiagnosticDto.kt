package com.printscript.snippets.execution.dto

data class DiagnosticDto(val ruleId: String, val message: String, val line: Int, val col: Int)
