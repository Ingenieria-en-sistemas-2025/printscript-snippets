package com.printscript.tests.dto

import java.time.Instant

data class TestCaseResponse(
    val id: Long,
    val snippetId: Long,
    val name: String,
    val inputs: List<String>?,
    val expectedOutputs: List<String>?,
    val targetVersionNumber: Long?,
    val lastRunStatus: String,
    val lastRunOutput: List<String>?,
    val lastRunAt: Instant?
)