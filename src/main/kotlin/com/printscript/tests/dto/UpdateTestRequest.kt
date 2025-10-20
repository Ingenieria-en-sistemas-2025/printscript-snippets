package com.printscript.tests.dto

data class UpdateTestRequest(
    val name: String?,
    val inputs: List<String>?,
    val expectedOutputs: List<String>?,
    val targetVersionNumber: Long?,
)
