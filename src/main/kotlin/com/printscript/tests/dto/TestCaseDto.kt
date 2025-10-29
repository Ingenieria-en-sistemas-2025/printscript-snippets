package com.printscript.tests.dto

data class TestCaseDto(
    val id: Long,
    val snippetId: Long,
    val name: String,
    val inputs: List<String>,
    val expectedOutputs: List<String>,
    val targetVersionNumber: Long?
)
