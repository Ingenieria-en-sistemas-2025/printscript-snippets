package com.printscript.tests.dto

data class CreateTestReq(
    val snippetId: String,
    val name: String,
    val inputs: List<String> = emptyList(),
    val expectedOutputs: List<String> = emptyList(),
    val targetVersionNumber: Long? = null
)
