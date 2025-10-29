package com.printscript.snippets.dto

data class CreateTestReq(
    val snippetId: Long,
    val name: String,
    val inputs: List<String> = emptyList(),
    val expectedOutputs: List<String> = emptyList(),
    val targetVersionNumber: Long? = null,
)
