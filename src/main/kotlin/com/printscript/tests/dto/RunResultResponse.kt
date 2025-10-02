package com.printscript.tests.dto

data class RunResultResponse(
    val passed: Boolean,
    val outputs: List<String>,
    val expected: List<String>,
    val status: String,
    val errorMessage: String?
)