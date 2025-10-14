package com.printscript.tests.dto

import java.time.Instant

data class TestRunResponse(
    val testId: Long,
    val snippetId: Long,
    val snippetVersionNumber: Long,
    val status: String,
    val inputs: List<String>?,
    val expectedOutputs: List<String>?,
    val outputs: List<String>?,
    val errorMessage: String?,
    val durationMs: Long?,
    val executedBy: String?,
    val executedAt: Instant
)
