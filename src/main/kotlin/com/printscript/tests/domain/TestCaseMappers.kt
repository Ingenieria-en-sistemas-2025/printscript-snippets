package com.printscript.tests.domain

import com.printscript.tests.dto.TestCaseResponse
import com.printscript.tests.dto.TestCaseBriefResponse

fun TestCaseEntity.toResponse() = TestCaseResponse(
    id = id!!,
    snippetId = snippetId,
    name = name,
    inputs = inputs,
    expectedOutputs = expectedOutputs,
    targetVersionNumber = targetVersionNumber,
    lastRunStatus = lastRunStatus,
    lastRunOutput = lastRunOutput,
    lastRunAt = lastRunAt
)

// version resumida del test, sin inputs ni outputs
fun TestCaseEntity.toBrief() = TestCaseBriefResponse(
    id = id!!,
    name = name,
    lastRunStatus = lastRunStatus,
    lastRunAt = lastRunAt?.toString()
)