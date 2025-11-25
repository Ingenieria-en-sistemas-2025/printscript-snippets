package com.printscript.snippets.dto

import java.util.UUID

data class TestCaseDto(
    val id: String,
    val snippetId: UUID,
    val name: String,
    val inputs: List<String>,
    val expectedOutputs: List<String>,
    val targetVersionNumber: Long?,
)
