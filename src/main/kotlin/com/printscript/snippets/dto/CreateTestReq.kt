package com.printscript.snippets.dto

import java.util.UUID

data class CreateTestReq(
    val snippetId: UUID? = null,
    val name: String,
    val inputs: List<String> = emptyList(),
    val expectedOutputs: List<String> = emptyList(),
    val targetVersionNumber: Long? = null,
)
