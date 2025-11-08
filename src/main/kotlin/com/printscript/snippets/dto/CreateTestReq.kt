package com.printscript.snippets.dto

data class CreateTestReq(
    val snippetId: String? = null,
    val name: String,
    val inputs: List<String> = emptyList(),
    val expectedOutputs: List<String> = emptyList(),
    val targetVersionNumber: Long? = null,
)
