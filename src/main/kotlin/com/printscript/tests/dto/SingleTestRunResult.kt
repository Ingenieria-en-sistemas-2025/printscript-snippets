package com.printscript.tests.dto

import com.printscript.tests.execution.dto.DiagnosticDto

data class SingleTestRunResult(
    val status: String, // "OK" | "MISMATCH" | "ERROR" (según tu Execution)
    val actual: List<String>?, // lo que devolvió Execution
    val expected: List<String>, // lo que leíste del TestCase
    val mismatchAt: Int?, // índice donde difiere (si aplica)
    val diagnostic: DiagnosticDto?, // si hubo error de runtime, etc.
)
