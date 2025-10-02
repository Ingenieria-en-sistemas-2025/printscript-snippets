package com.printscript.tests.domain

import TestCaseEntity
import com.printscript.tests.dto.TestCaseResponse

fun TestCaseEntity.toResponse(json: JsonUtils) = TestCaseResponse(
    id = id,
    snippetId = snippetId,
    name = name,
    inputs = json.jsonToList(inputs),
    expectedOutputs = json.jsonToList(expectedOutputs),
    targetVersionNumber = targetVersionNumber,
    lastRunStatus = lastRunStatus,
    lastRunOutput = json.jsonToList(lastRunOutput),
    lastRunAt = lastRunAt
)
