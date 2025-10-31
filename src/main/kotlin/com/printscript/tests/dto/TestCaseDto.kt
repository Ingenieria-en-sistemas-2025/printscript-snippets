package com.printscript.tests.dto

data class TestCaseDto(
    val id: String,
    val snippetId: String,
    val name: String,
    val inputs: List<String>,
    val expectedOutputs: List<String>,
    val targetVersionNumber: Long?,
)
